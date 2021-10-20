package com.malliina.sbtutils

import sbt._

object SbtUtils extends SbtUtils

trait SbtUtils {
  val logbackModules = Seq("classic", "core")

  def loggingDeps = logbackModules.map { m =>
    "ch.qos.logback" % s"logback-$m" % "1.2.6"
  } ++ Seq(
    "org.slf4j" % "slf4j-api" % "1.7.32"
  )
}
