package com.malliina.bundler

import sbt.taskKey

object BundlerKeys {
  val start = taskKey[Unit]("Starts the project")
}
