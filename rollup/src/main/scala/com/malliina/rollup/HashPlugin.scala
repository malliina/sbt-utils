package com.malliina.rollup

import com.malliina.build.FileIO
import com.malliina.storage.{StorageLong, StorageSize}
import org.apache.ivy.util.ChecksumHelper
import sbt.*
import sbt.Keys.{streams, target}

import java.nio.file.{Files, Path}
import scala.jdk.CollectionConverters.*

object HashPlugin extends AutoPlugin {
  val algorithm = "md5"

  object autoImport {
    val hashIncludeExts = settingKey[Seq[String]]("Extensions to hash")
    val hashRoot = settingKey[Path]("Root dir")
    val hashAssets = taskKey[Seq[HashedFile]]("Hashed files")
    val hashPackage = settingKey[String]("Package name for assets file")
    val hash = taskKey[Seq[Path]]("Create hash")
    val useHash = settingKey[Boolean]("Use hashed paths")
    val copyFolders = settingKey[Seq[Path]]("Copy folders")
    val copy = taskKey[Seq[Path]]("Copies folders")
    val dataUriLimit = settingKey[StorageSize]("Maximum asset size for data URI inlining")
  }
  import autoImport.*
  override val projectSettings: Seq[Def.Setting[?]] = Seq(
    useHash := true,
    copyFolders := Nil,
    copy := {
      val root = hashRoot.value
      copyFolders.value
        .toSet[Path]
        .flatMap { dir =>
          val dirPath = dir
          allPaths(dirPath).flatMap { path =>
            val rel = dirPath.relativize(path)
            val dest = root.resolve(rel)
            if (Files.isRegularFile(path)) {
              FileIO.copyIfChanged(path, dest)
              Option(dest)
            } else if (Files.isDirectory(path)) { Option(Files.createDirectories(dest)) }
            else None
          }
        }
        .toList
    },
    dataUriLimit := 48.kilos,
    hashIncludeExts := Seq(".css", ".js", ".jpg", ".jpeg", ".png", ".svg", ".ico"),
    hashPackage := "com.malliina.assets",
    hashAssets := {
      val log = streams.value.log
      val root = hashRoot.value
      val enabled = useHash.value
      val exts = hashIncludeExts.value
      allPaths(root).filter { p =>
        val name = p.getFileName.toString
        Files.isRegularFile(p) &&
        exts.exists(ext => name.endsWith(ext)) &&
        name.count(c => c == '.') < 2
      }.map { file =>
        val hashed = if (enabled) prepFile(file, log) else file
        HashedFile.from(file, hashed, root)
      }
    },
    hashAssets := hashAssets
      .dependsOn(copy, Def.task(Files.createDirectories(hashRoot.value)))
      .value,
    hash := {
      val hashes = hashAssets.value
      val hashesEnabled = useHash.value
      val cached = FileFunction.cached(streams.value.cacheDirectory / "assets") { in =>
        val file = makeAssetsFile(
          target.value,
          hashPackage.value,
          "assets",
          hashes,
          makeDataUris = hashesEnabled,
          dataUriLimit = dataUriLimit.value
        )
        Set(file)
      }
      cached(hashes.map(_.hashedFile.toFile).toSet).toSeq.map(_.toPath)
    }
  )

  def prepFile(file: Path, log: Logger) = {
    val checksum = ChecksumHelper.computeAsString(file.toFile, algorithm)
    val checksumFile = file.getParent.resolve(s"${file.getFileName}.$algorithm")
    if (!Files.exists(checksumFile)) {
      Files.writeString(checksumFile, checksum)
      log.debug(s"Wrote $checksumFile.")
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
    makeDataUris: Boolean,
    dataUriLimit: StorageSize
  ): File = {
    val inlined = hashes.map(h => s""""${h.path}" -> "${h.hashedPath}"""").mkString(", ")
    val dataUris =
      if (makeDataUris)
        hashes
          .filter(h => h.size <= dataUriLimit)
          .map { h =>
            val dataUri = FileIO.dataUri(h.originalFile)
            s""""${h.path}" -> "$dataUri""""
          }
          .mkString(", ")
      else ""
    val objectName = "HashedAssets"
    val content =
      s"""
         |package $packageName
         |
         |object $objectName {
         |  val prefix: String = "$prefix"
         |  val assets: Map[String, String] = Map($inlined)
         |  val dataUris: Map[String, String] = Map($dataUris)
         |}
         |""".stripMargin.trim + sbt.io.IO.Newline
    val destFile = destDir(base, packageName) / s"$objectName.scala"
    FileIO.writeIfChanged(content, destFile.toPath)
    destFile
  }

  def destDir(base: File, packageName: String): File =
    packageName.split('.').foldLeft(base)((acc, part) => acc / part)

  def allPaths(root: Path) =
    if (Files.exists(root)) FileIO.using(Files.walk(root))(_.iterator().asScala.toList)
    else Nil
}
