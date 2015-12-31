import sbt._
import sbt.Keys._
import bintray.Plugin.bintraySettings
import bintray.Keys.{bintrayOrganization, repository, bintray}

/**
 * A scala build file template.
 */
object SbtUtilsBuild extends Build {

  lazy val template = Project("sbt-utils", file(".")).settings(projectSettings: _*)

  lazy val projectSettings = bintraySettings ++ Seq(
    organization := "com.malliina",
    version := "0.3.0",
    sbtPlugin := true,
    scalaVersion := "2.10.6",
    exportJars := false,
    fork in Test := true,
    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % "2.2.5" % "test"
    ),
    bintrayOrganization in bintray := None,
    repository in bintray := "sbt-plugins",
    publishMavenStyle := false,
    licenses += ("MIT", url("http://opensource.org/licenses/MIT"))
  ) ++ plugins

  def plugins = Seq(
    "com.jsuereth" % "sbt-pgp" % "1.0.0",
    "org.xerial.sbt" % "sbt-sonatype" % "0.2.2"
  ) map addSbtPlugin
}
