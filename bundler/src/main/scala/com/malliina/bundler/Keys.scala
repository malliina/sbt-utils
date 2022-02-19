package com.malliina.bundler

import sbt.taskKey

object Keys {
  val start = taskKey[Unit]("Starts the project")
  val startInc = taskKey[Unit]("Starts the project, conditionally")
}
