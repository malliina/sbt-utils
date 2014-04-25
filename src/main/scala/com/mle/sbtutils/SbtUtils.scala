package com.mle.sbtutils

import sbt.Keys._
import sbt._
import xerial.sbt.Sonatype
import com.typesafe.sbt.pgp.PgpKeys

/**
 *
 * @author mle
 */
trait SbtUtils {
  private val lineSep = sys.props("line.separator")

  val gitUserName = settingKey[String]("Git username")
  val developerName = settingKey[String]("Developer name")
  // has defaults
  val sonatypeCredentials = settingKey[File]("Path to sonatype credentials, defaults to ~/.ivy2/sonatype.txt")
  val gitProjectName = settingKey[String]("Project name on GitHub, defaults to the project name")
  val developerHomePageUrl = settingKey[String]("Developer home page URL, defaults to the GitHub project page")
  val sbtUtilsHelp = taskKey[Unit]("Shows help")
  //  val publishRelease = taskKey[Unit]("publishSigned followed by sonatypeRelease")

  def testableProject(name: String): Project = Project(name, file(".")).settings(Seq(
    libraryDependencies += "org.scalatest" %% "scalatest" % "2.1.3" % "test"
  ): _*)

  def logProject(name: String): Project = testableProject(name)
    .settings(Seq(libraryDependencies ++= loggingDeps): _*)

  def loggingDeps = Seq(
    "org.slf4j" % "slf4j-api" % "1.7.7",
    "ch.qos.logback" % "logback-classic" % "1.1.2",
    "ch.qos.logback" % "logback-core" % "1.1.2"
  )

  lazy val publishSettings = Sonatype.sonatypeSettings ++ Seq(
    organization := s"com.github.${gitUserName.value}",
    gitProjectName := name.value,
    developerHomePageUrl := s"https://github.com/${gitUserName.value}/${gitProjectName.value}",
    sonatypeCredentials := Path.userHome / ".ivy2" / "sonatype.txt",
    credentials ++= creds(sonatypeCredentials.value),
    pomExtra := SbtGit.gitPom(gitProjectName.value, gitUserName.value, developerName.value, developerHomePageUrl.value),
    publishArtifact in Test := false,
    sbtUtilsHelp := {
      val msg = describe(sbtUtilsHelp, gitUserName, developerName, sonatypeCredentials, gitProjectName, developerHomePageUrl)
      streams.value.log.info(msg)
    }
    // http://stackoverflow.com/a/19297441
    //    publishRelease := {
    //      val unused = PgpKeys.publishSigned.value
    //      val unused2 = Sonatype.SonatypeKeys.sonatypeRelease.value
    //      ()
    //    }
  )

  private def creds(file: File): Seq[DirectCredentials] =
    toSeq(Credentials.loadCredentials(file))

  def toSeq[A, B](either: Either[A, B]) =
    either.fold(err => Seq.empty, value => Seq(value))

  def describe(tasks: ScopedTaskable[_]*) = tasks.map(_.key).map(t => {
    val tabCount = t.label.size match {
      case i if i > 16 => 1
      case i if i > 8 => 2
      case _ => 3
    }
    val sep = (1 to tabCount).map(_ => "\t").mkString
    t.label + sep + t.description.getOrElse("No description")
  }).mkString(lineSep)
}

object SbtUtils extends SbtUtils
