package com.malliina.sbt.filetree

import sbt.{File, settingKey}

object FileTreeKeys {
  val fileTreeSources =
    settingKey[Seq[DirMap]]("File tree source directories and generated objects")
}

case class DirMap(source: File, destination: String, mapFunc: String = "identity") {
  val (packageName, className) = DirMap.splitAtLastDot(destination)
}

object DirMap {
  def splitAtLastDot(in: String): (String, String) = {
    val dot = in.lastIndexOf('.')
    if (dot < 0) ("filetree", in) else (in.substring(0, dot), in.substring(dot + 1))
  }
}
