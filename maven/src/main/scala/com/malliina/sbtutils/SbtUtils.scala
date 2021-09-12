package com.malliina.sbtutils

import sbt._

object SbtUtils extends SbtUtils

trait SbtUtils {
  def loggingDeps = Seq(
    "org.slf4j" % "slf4j-api" % "1.7.32",
    "ch.qos.logback" % "logback-classic" % "1.2.5",
    "ch.qos.logback" % "logback-core" % "1.2.5"
  )
}
