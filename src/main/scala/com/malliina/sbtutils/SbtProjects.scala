package com.malliina.sbtutils

import sbt.Keys._
import sbt._

object SbtProjects {
  def mavenPublishProject(name: String) =
    testableProject(name).settings(SbtUtils.mavenSettings: _*)

  def sbtPlugin(name: String) =
    testableProject(name).settings(SbtUtils.pluginSettings: _*)

  def logProject(name: String): Project =
    testableProject(name)
      .settings(Seq(libraryDependencies ++= SbtUtils.loggingDeps): _*)

  def testableProject(name: String, base: File = file(".")): Project =
    Project(name, base).settings(scalaTestSettings: _*)

  def scalaTestSettings = Seq(
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.3" % Test
  )
}
