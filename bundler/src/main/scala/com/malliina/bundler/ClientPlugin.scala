package com.malliina.bundler

import org.apache.ivy.util.ChecksumHelper
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport.*
import org.scalajs.sbtplugin.Stage
import sbt.Keys._
import sbt._
import sbt.internal.util.ManagedLogger
import scalajsbundler.sbtplugin.ScalaJSBundlerPlugin
import scalajsbundler.sbtplugin.ScalaJSBundlerPlugin.autoImport.*

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path, StandardCopyOption}

case class HashedFile(path: String, hashedPath: String, originalFile: Path, hashedFile: Path)

object ClientPlugin extends AutoPlugin {
  override def requires: Plugins = ScalaJSBundlerPlugin && FileInputPlugin
  object autoImport {
    val assetsPackage = settingKey[String]("Package name of generated assets file")
    val assetsDir = settingKey[Path]("Webpack assets dir to serve in server")
    val assetsPrefix = settingKey[String]("Assets prefix")
    val assetsRoot = settingKey[Path]("Public assets dir")
    val prepTarget = taskKey[Path]("Prep target dir")
    val hashAssets = taskKey[Seq[HashedFile]]("Hashed files")
    val allAssets = taskKey[Seq[File]]("Hashed and non-hashed files")
    val writeAssets = taskKey[Seq[File]]("Writes the assets metadata source file")
  }
  val start = Keys.start
  import autoImport._

  override def projectSettings: Seq[Def.Setting[_]] =
    stageSettings(Stage.FastOpt) ++ stageSettings(Stage.FullOpt) ++ Seq(
      webpack / version := "5.65.0",
      webpackCliVersion := "4.9.1",
      startWebpackDevServer / version := "4.5.0",
      webpackEmitSourceMaps := false,
      scalaJSUseMainModuleInitializer := true,
      fastOptJS / webpackConfigFile := Some(baseDirectory.value / "webpack.dev.config.js"),
      fullOptJS / webpackConfigFile := Some(baseDirectory.value / "webpack.prod.config.js"),
      Compile / fastOptJS / webpackBundlingMode := BundlingMode.LibraryOnly(),
      Compile / fullOptJS / webpackBundlingMode := BundlingMode.Application,
      assetsPackage := "com.malliina.assets",
      assetsDir := (target.value / "assets").toPath,
      assetsPrefix := "public",
      assetsRoot := assetsDir.value.resolve(assetsPrefix.value),
      prepTarget := Files.createDirectories(assetsRoot.value),
      start := Def.taskIf {
        val hasChanges = start.inputFileChanges.hasChanges
        if (hasChanges) {
          (Compile / fastOptJS / writeAssets).map(_ => ()).value
        } else {
          Def.task(streams.value.log.debug(s"No changes to ${name.value}.")).value
        }
      }.value
    )

  private def stageSettings(stage: Stage): Seq[Def.Setting[_]] = {
    val stageTask = stage match {
      case Stage.FastOpt => fastOptJS
      case Stage.FullOpt => fullOptJS
    }
    Seq(
      (Compile / stageTask / hashAssets) := {
        val files = (Compile / stageTask / webpack).value
        val log = streams.value.log
        files.flatMap {
          file =>
            val root = assetsRoot.value
            val relativeFile = file.data.relativeTo(root.toFile).get
            val dest = file.data.toPath
            val extraFiles =
              if (!relativeFile.toPath.startsWith("static")) {
                val hashed = prepFile(dest, log)
                List(
                  HashedFile(
                    root.relativize(dest).toString.replace('\\', '/'),
                    root.relativize(hashed).toString.replace('\\', '/'),
                    dest,
                    hashed
                  )
                )
              } else {
                Nil
              }
            extraFiles
        }
      },
      Compile / stageTask / webpack := {
        val files = (Compile / stageTask / webpack).value
        val log = streams.value.log
        files.map { file =>
          val relativeFile = file.data.relativeTo((Compile / npmUpdate / crossTarget).value).get
          val dest = assetsRoot.value.resolve(relativeFile.toPath)
          val path = file.data.toPath
          Files.createDirectories(dest.getParent)
          Files.copy(path, dest, StandardCopyOption.REPLACE_EXISTING)
          log.debug(s"Wrote '$dest', ${Files.size(path)} bytes.")
          Files.createDirectories(dest.getParent)
          file.copy(dest.toFile)(file.metadata)
        }
      },
      Compile / stageTask / webpack := (Compile / stageTask / webpack).dependsOn(prepTarget).value,
      Compile / stageTask / allAssets := {
        val webpackFiles =
          (Compile / stageTask / webpack).value.map(_.data)
        val hashedFiles =
          (Compile / stageTask / hashAssets).value.map(_.hashedFile.toFile)
        webpackFiles ++ hashedFiles
      },
      Compile / stageTask / writeAssets := {
        val dest = target.value
        val hashed = (Compile / stageTask / hashAssets).value
        val prefix = assetsPrefix.value
        val log = streams.value.log
        val cached = FileFunction.cached(streams.value.cacheDirectory / "assets") { in =>
          makeAssetsFile(dest, assetsPackage.value, prefix, hashed, log)
        }
        cached(hashed.map(_.hashedFile.toFile).toSet).toSeq
      }
    )
  }

  def prepFile(file: Path, log: ManagedLogger) = {
    val algorithm = "md5"
    val checksum = ChecksumHelper.computeAsString(file.toFile, algorithm)
    val checksumFile = file.getParent.resolve(s"${file.getFileName}.$algorithm")
    if (!Files.exists(checksumFile)) {
      Files.writeString(checksumFile, checksum)
      log.info(s"Wrote $checksumFile.")
    }
    val (base, ext) = file.toFile.baseAndExt
    val hashedFile = file.getParent.resolve(s"$base.$checksum.$ext")
    if (!Files.exists(hashedFile)) {
      Files.copy(file, hashedFile)
      log.info(s"Wrote $hashedFile.")
    }
    hashedFile
  }

  def makeAssetsFile(
    base: File,
    packageName: String,
    prefix: String,
    hashes: Seq[HashedFile],
    log: ManagedLogger
  ): Set[File] = {
    val inlined = hashes.map(h => s""""${h.path}" -> "${h.hashedPath}"""").mkString(", ")
    val objectName = "HashedAssets"
    val content =
      s"""
         |package $packageName
         |
         |object $objectName {
         |  val prefix: String = "$prefix"
         |  val assets: Map[String, String] = Map($inlined)
         |}
         |""".stripMargin.trim + IO.Newline
    val destFile = destDir(base, packageName) / s"$objectName.scala"
    IO.write(destFile, content, StandardCharsets.UTF_8)
    log.info(s"Wrote $destFile.")
    Set(destFile)
  }

  def destDir(base: File, packageName: String): File =
    packageName.split('.').foldLeft(base)((acc, part) => acc / part)
}
