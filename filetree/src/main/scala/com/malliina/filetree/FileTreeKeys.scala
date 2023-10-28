package com.malliina.filetree

import sbt.settingKey

import java.nio.file.Path

object FileTreeKeys {
  val fileTreeSources =
    settingKey[Seq[DirMap]]("File tree source directories and generated objects")
  val scalafmtConf = settingKey[Option[Path]]("Path to .scalafmt.conf")
}

case class DirMap(source: Path, destination: String, mapFunc: String = "identity") {
  val (packageName, className) = DirMap.splitAtLastDot(destination)
}

object DirMap {
  def splitAtLastDot(in: String): (String, String) = {
    val dot = in.lastIndexOf('.')
    if (dot < 0) ("filetree", in) else (in.substring(0, dot), in.substring(dot + 1))
  }
}
