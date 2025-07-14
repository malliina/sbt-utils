package com.malliina.rollup

import com.malliina.build.FileIO
import com.malliina.nodejs.PathIO
import com.malliina.rollup.CommonKeys.{assetsPrefix, assetsRoot, build}
import org.apache.ivy.util.ChecksumHelper
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport.{ModuleKind, fastLinkJS, fullLinkJS, scalaJSLinkerConfig, scalaJSLinkerOutputDirectory, scalaJSStage, scalaJSUseMainModuleInitializer}
import org.scalajs.sbtplugin.{ScalaJSPlugin, Stage}
import sbt.{IO => _, *}
import sbt.Keys.*

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import scala.collection.JavaConverters.asScalaBufferConverter

object EsbuildPlugin extends AutoPlugin {
  val utf8 = StandardCharsets.UTF_8
  val sha1 = "sha1"

  sealed abstract class Loader(val name: String) {
    override def toString: String = name
  }
  object Loader {
    case object DataUrl extends Loader("dataurl")
    case object File extends Loader("file")
    case object Copy extends Loader("copy")
    case object Base64 extends Loader("base64")
    case class Other(n: String) extends Loader(n)
  }
  val defaultLoaders: Map[String, Loader] = Seq("woff", "woff2", "png", "svg").map { ext =>
    s".$ext" -> Loader.DataUrl
  }.toMap

  override def requires: Plugins = ScalaJSPlugin

  object autoImport {
    val resourceDir = settingKey[Path]("Source directory with package.json etc.")
    val npmRoot = settingKey[Path]("Working dir for npm commands")
    val copyBuildResources = taskKey[Unit]("Copies files")
    val stageFiles = taskKey[Unit]("Stages files")
    val stageMainJs = taskKey[Unit]("Stages scala.js output")
    val configureEsbuild = taskKey[Unit]("Prepares esbuild")
    val loaders = settingKey[Map[String, Loader]]("Esbuild loaders (extension to loader)")
  }
  import autoImport.*

  override def globalSettings: Seq[Setting[?]] = Seq(
    commands += Command.args("mode", "<mode>") { (state, args) =>
      val newStage = args.toList match {
        case h :: Nil =>
          h match {
            case "prod" => Stage.FullOpt
            case "dev"  => Stage.FastOpt
            case other  => sys.error(s"Invalid mode: '$other'.")
          }
        case other => sys.error("Specify either dev or prod as the only argument.")
      }
      state.appendWithoutSession(
        Seq(
          Global / scalaJSStage := newStage
        ),
        state
      )
    }
  )

  override val projectSettings = Seq(
    scalaJSUseMainModuleInitializer := true,
    scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.ESModule) },
    assetsPrefix := "assets",
    loaders := defaultLoaders,
    resourceDir := (Compile / resourceDirectory).value.toPath,
    npmRoot := ((Compile / crossTarget).value / "stage").toPath,
    assetsRoot := npmRoot.value / "assets",
    copyBuildResources := FileIO.copyDir(resourceDir.value, npmRoot.value),
    stageFiles := {
      IO.writePackageJsonIfChanged(resourceDir.value, npmRoot.value, "package.esbuild.json")
    },
    stageFiles := stageFiles.dependsOn(copyBuildResources).value,
    build := Def.settingDyn {
      val stageTask = scalaJSStage.value match {
        case Stage.FastOpt => fastLinkJS
        case Stage.FullOpt => fullLinkJS
      }
      stageTask / build
    }.value
  ) ++ stageSettings(Stage.FastOpt) ++ stageSettings(Stage.FullOpt)

  private def stageSettings(stage: Stage): Seq[Setting[?]] = {
    val stageTask = stage match {
      case Stage.FastOpt => fastLinkJS
      case Stage.FullOpt => fullLinkJS
    }
    Seq(
      stageTask / stageMainJs := {
        val report = (Compile / stageTask).value.data
        val mainJs = report.publicModules
          .find(_.moduleID == "main")
          .getOrElse(sys.error(s"Module 'main' not found."))
        val jsFile =
          (Compile / stageTask / scalaJSLinkerOutputDirectory).value.toPath / mainJs.jsFileName
        FileIO.copyIfChanged(jsFile, npmRoot.value / mainJs.jsFileName)
      },
      stageTask / configureEsbuild := {
        val report = (Compile / stageTask).value.data
        val mainJs = report.publicModules
          .find(_.moduleID == "main")
          .getOrElse(sys.error(s"Module 'main' not found."))
        val entrypoint = mainJs.jsFileName
        val out = npmRoot.value.relativize(assetsRoot.value)
        streams.value.log.info(s"configuring with ${mainJs.jsFileName} to $out")
        val loadersJs = loaders.value.map { case (ext, l) =>
          s"""'$ext': '$l'"""
        }.mkString(", ")
        val minify = stage == Stage.FullOpt
        val script = s"""
           |const path = require('path');
           |const esbuildOptions = {
           |  platform: 'browser',
           |  entryPoints: ['$entrypoint'],
           |  bundle: true,
           |  minify: $minify,
           |  outdir: path.normalize('$out'),
           |  metafile: true,
           |  logLevel: 'info',
           |  loader: { $loadersJs },
           |}
           |const bundle = async () => {
           |  const esbuild = require('esbuild');
           |  const fs = require('fs');
           |
           |  const result = await esbuild.build(esbuildOptions);
           |  return result.metafile
           |}
           |
           |bundle()
           |""".stripMargin
        val scriptFile = npmRoot.value / "esbuild.cjs"
        FileIO.writeIfChanged(script, scriptFile)
      },
      stageTask / build := {
        val log = streams.value.log
        val cwd = npmRoot.value
        val packageJson = cwd / "package.json"
        val cacheFile = cwd / "package.json.sha1"
        val checksum = computeChecksum(packageJson)
        val isProd = false
        if (
          Files.exists(cacheFile) && Files
            .readAllLines(cacheFile, utf8)
            .asScala
            .headOption
            .contains(checksum)
        ) {
          npmRunBuild(cwd, log)
        } else {
          FileIO.writeIfChanged(checksum, cacheFile)
          if (stage == Stage.FullOpt) npmCi(cwd, log)
          else {
            npmInstall(cwd, log)
            val lockFile = resourceDir.value / "package-lock.json"
            val newestLockFile = cwd / "package-lock.json"
            if (Files.exists(newestLockFile)) {
              FileIO.copyIfChanged(newestLockFile, lockFile)
            }
          }
          npmRunBuild(cwd, log)
        }
      },
      stageTask / build := (stageTask / build)
        .dependsOn(stageTask / configureEsbuild)
        .dependsOn(stageTask / stageMainJs)
        .dependsOn(Compile / stageTask)
        .dependsOn(stageFiles)
        .value
    )
  }

  def npmRunBuild(cwd: Path, log: Logger) =
    process(Seq("npm", "run", "build"), cwd, log)

  def npmInstall(cwd: Path, log: Logger) =
    process(Seq("npm", "install"), cwd, log)

  def npmCi(cwd: Path, log: Logger) =
    process(Seq("npm", "ci"), cwd, log)

  def process(commands: Seq[String], cwd: Path, log: Logger) =
    PathIO.runProcessSync(commands, cwd, log)

  def computeChecksum(file: Path) = ChecksumHelper.computeAsString(file.toFile, sha1)
}
