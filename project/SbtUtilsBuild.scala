import sbt._
import sbt.Keys._
import bintray.Plugin.bintraySettings
import bintray.Keys.{bintrayOrganization, repository, bintray}

object SbtUtilsBuild {

  lazy val template = Project("sbt-utils", file(".")).settings(projectSettings: _*)

  lazy val projectSettings = bintraySettings ++ Seq(
    organization := "com.malliina",
    sbtPlugin := true,
    scalaVersion := "2.10.6",
    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % "3.0.0" % Test
    ),
    bintrayOrganization in bintray := None,
    repository in bintray := "sbt-plugins",
    publishMavenStyle := false,
    licenses += ("MIT", url("http://opensource.org/licenses/MIT"))
  ) ++ plugins

  def plugins = Seq(
    "com.jsuereth" % "sbt-pgp" % "1.0.0",
    "org.xerial.sbt" % "sbt-sonatype" % "1.1",
    "com.github.gseitz" % "sbt-release" % "1.0.3",
    "me.lessis" % "bintray-sbt" % "0.2.1"
  ) map addSbtPlugin
}
