package com.malliina.sbtutils

import sbt._

object SbtUtils extends SbtUtils

trait SbtUtils {
  def loggingDeps = Seq("classic", "core").map { m =>
    "ch.qos.logback" % s"logback-$m" % "1.4.5"
  }
}
