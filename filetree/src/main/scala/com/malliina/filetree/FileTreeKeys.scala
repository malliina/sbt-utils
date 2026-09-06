package com.malliina.filetree

import sbt.{File, settingKey, taskKey}

import java.nio.file.Path

object FileTreeKeys:
  val fileTreeSources =
    settingKey[Seq[DirMap]]("File tree source directories and generated objects")
  val scalafmtConf = settingKey[Option[Path]]("Path to .scalafmt.conf")
  val writeFileTree = taskKey[Seq[File]]("Writes the file tree.")

case class DirMap(source: Path, destination: String, mapFunc: String = "identity"):
  val (packageName, className) = DirMap.splitAtLastDot(destination)

object DirMap:
  private def splitAtLastDot(in: String): (String, String) =
    val dot = in.lastIndexOf('.')
    if dot < 0 then ("filetree", in) else (in.substring(0, dot), in.substring(dot + 1))
