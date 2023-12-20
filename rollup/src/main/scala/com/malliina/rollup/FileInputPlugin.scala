package com.malliina.rollup

import sbt.*
import sbt.Keys.{resourceDirectories, sourceDirectories}
import sbt.nio.Keys.fileInputs

object FileInputPlugin extends AutoPlugin {
  val start = CommonKeys.start
  object autoImport {
    val dependentModule = settingKey[Project]("Module depended on")
  }
  import autoImport.dependentModule

  override def projectSettings: Seq[Def.Setting[?]] = Seq(
    start / fileInputs ++=
      (Compile / sourceDirectories).value.map(_.toGlob / ** / "*.scala") ++
        (Compile / resourceDirectories).value.map(_.toGlob / **),
    start / fileInputs ++= Def
      .settingDyn(dependentModule.value / Compile / sourceDirectories)
      .value
      .map(_.toGlob / ** / "*.scala") ++ Def
      .settingDyn(dependentModule.value / Compile / resourceDirectories)
      .value
      .map(_.toGlob / **)
  )
}
