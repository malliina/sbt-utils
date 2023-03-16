package com.malliina.rollup

import com.malliina.storage.StorageLong
import sbt.{settingKey, taskKey}

import java.nio.file.{Files, Path}
import scala.sys.process.Process
import scala.util.Try

object CommonKeys {
  val assetsRoot = settingKey[Path]("Assets root directory")
  val build = taskKey[Unit]("Builds app") // Consider replacing with compile
  val isProd = settingKey[Boolean]("true if in prod mode, false otherwise")
  val start = taskKey[Unit]("Starts the project")
}

object Git {
  def gitHash: String =
    sys.env
      .get("GITHUB_SHA")
      .orElse(Try(Process("git rev-parse HEAD").lineStream.head).toOption)
      .getOrElse("unknown")
}

case class HashedFile(path: String, hashedPath: String, originalFile: Path, hashedFile: Path) {
  def size = Files.size(originalFile).bytes
}

object HashedFile {
  def from(original: Path, hashed: Path, root: Path) = HashedFile(
    root.relativize(original).toString.replace('\\', '/'),
    root.relativize(hashed).toString.replace('\\', '/'),
    original,
    hashed
  )
}
