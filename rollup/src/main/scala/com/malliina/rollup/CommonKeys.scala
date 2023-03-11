package com.malliina.rollup

import sbt.{settingKey, taskKey}

import java.nio.file.Path

object CommonKeys {
  val assetsRoot = settingKey[Path]("Assets root directory")
  val build = taskKey[Unit]("Builds app") // Consider replacing with compile
  val isProd = settingKey[Boolean]("true if in prod mode, false otherwise")
  val start = taskKey[Unit]("Starts the project")
}
