package com.malliina.sbtutils

import sbt.Keys._
import sbt._

object SbtProjects {
  def testableProject(name: String, base: File = file(".")): Project =
    Project(name, base).settings(scalaTestSettings: _*)

  def scalaTestSettings = Seq(
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.0" % Test
  )

  def logProject(name: String): Project = testableProject(name)
    .settings(Seq(libraryDependencies ++= SbtUtils.loggingDeps): _*)

  def mavenPublishProject(name: String) =
    testableProject(name).settings(SbtUtils.mavenSettings: _*)
}
