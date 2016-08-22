package com.malliina.sbtutils

import sbt.Keys._
import sbt._

object SbtProjects {
  def testableProject(name: String, base: File = file(".")): Project =
    baseProject(name, base).settings(scalaTestSettings: _*)

  def baseProject(name: String, base: File = file(".")): Project =
    Project(name, base).settings(baseSettings: _*)

  def scalaTestSettings = Seq(
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.0" % Test
  )

  def baseSettings = Seq(
    // includes scala-xml for 2.11 but excludes it for 2.10 (required by scalatest)
    // see http://www.scala-lang.org/news/2014/03/06/release-notes-2.11.0-RC1.html
    libraryDependencies := {
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, scalaMajor)) if scalaMajor >= 11 =>
          libraryDependencies.value :+ "org.scala-lang.modules" %% "scala-xml" % "1.0.3"
        case _ =>
          libraryDependencies.value
      }
    }
  )

  def logProject(name: String): Project = testableProject(name)
    .settings(Seq(libraryDependencies ++= SbtUtils.loggingDeps): _*)

  def mavenPublishProject(name: String) = testableProject(name).settings(SbtUtils.mavenSettings: _*)
}
