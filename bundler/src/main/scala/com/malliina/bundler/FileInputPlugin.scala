package com.malliina.bundler

import sbt._
import sbt.Keys.{resourceDirectories, sourceDirectories}
import sbt.nio.Keys.fileInputs

object FileInputPlugin extends AutoPlugin {
  val start = Keys.start
  override def projectSettings: Seq[Def.Setting[_]] = Seq(
    start / fileInputs ++=
      (Compile / sourceDirectories).value.map(_.toGlob / ** / "*.scala") ++
        (Compile / resourceDirectories).value.map(_.toGlob / **)
    )
}
