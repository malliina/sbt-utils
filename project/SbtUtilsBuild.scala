import sbt._
import sbt.Keys._
import xerial.sbt.Sonatype

/**
 * A scala build file template.
 */
object SbtUtilsBuild extends Build {

  lazy val template = Project("sbt-utils", file(".")).settings(projectSettings: _*)

  lazy val projectSettings = Sonatype.sonatypeSettings ++ Seq(
    organization := "com.github.malliina",
    version := "0.0.6",
    sbtPlugin := true,
    scalaVersion := "2.10.4",
    //    crossScalaVersions := Seq("2.11.0", "2.10.4"),
    exportJars := false,
    fork in Test := true,
    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % "2.2.3" % "test"
    ),
    pomExtra := SbtGit.gitPom(name.value, "malliina", "Michael Skogberg", "http://mskogberg.info"),
    credentials ++= creds(Path.userHome / ".ivy2" / "sonatype.txt")
  ) ++ plugins

  def plugins = Seq(
    "com.jsuereth" % "sbt-pgp" % "1.0.0",
    "org.xerial.sbt" % "sbt-sonatype" % "0.2.1"
  ) map addSbtPlugin

  private def creds(file: File): Seq[DirectCredentials] =
    toSeq(Credentials.loadCredentials(file))

  def toSeq[A, B](either: Either[A, B]) =
    either.fold(err => Seq.empty, value => Seq(value))
}