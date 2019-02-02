package com.malliina.sbtutils

import sbt._

object SbtUtils extends SbtUtils

trait SbtUtils {
  def loggingDeps = Seq(
    "org.slf4j" % "slf4j-api" % "1.7.25",
    "ch.qos.logback" % "logback-classic" % "1.2.3",
    "ch.qos.logback" % "logback-core" % "1.2.3"
  )
}
