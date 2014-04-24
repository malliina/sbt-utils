import sbt._
import sbt.Keys._

object BuildBuild extends Build {

  override lazy val settings = super.settings ++ Seq(
    scalaVersion := "2.10.4"
  ) ++ sbtPlugins

  def sbtPlugins = Seq(
    "com.typesafe.sbt" % "sbt-pgp" % "0.8.1",
    "org.xerial.sbt" % "sbt-sonatype" % "0.2.1"
  ) map addSbtPlugin

  override lazy val projects = Seq(root)
  lazy val root = Project("plugins", file("."))
}