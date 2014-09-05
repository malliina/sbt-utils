package com.mle.sbtutils

import sbt.Keys._
import sbt._

/**
 * @author Michael
 */
object SbtProjects {
  def testableProject(name: String): Project = Project(name, file(".")).settings(Seq(
    libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.0" % "test",
    // includes scala-xml for 2.11 but excludes it for 2.10 (required by scalatest)
    // see http://www.scala-lang.org/news/2014/03/06/release-notes-2.11.0-RC1.html
    libraryDependencies := {
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, scalaMajor)) if scalaMajor >= 11 =>
          libraryDependencies.value :+ "org.scala-lang.modules" %% "scala-xml" % "1.0.1"
        case _ =>
          libraryDependencies.value
      }
    }
  ): _*)

  def logProject(name: String): Project = testableProject(name)
    .settings(Seq(libraryDependencies ++= SbtUtils.loggingDeps): _*)

  def mavenPublishProject(name: String) = testableProject(name).settings(SbtUtils.publishSettings: _*)
}
