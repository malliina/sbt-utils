package com.malliina.sbtutils

import com.typesafe.sbt.pgp.PgpKeys
import sbt.Keys._
import sbt._
import sbtrelease.ReleasePlugin
import sbtrelease.ReleaseStateTransformations._
import xerial.sbt.Sonatype

object MavenCentralKeys {
  val gitUserName = settingKey[String]("Git username")
  val developerName = settingKey[String]("Developer name")
  // has defaults
  val sonatypeCredentials =
    settingKey[File]("Path to sonatype credentials, defaults to ~/.ivy2/sonatype.txt")
  val gitProjectName = settingKey[String]("Project name on GitHub, defaults to the project name")
  val developerHomePageUrl =
    settingKey[String]("Developer home page URL, defaults to the GitHub project page")
  val beforePublish = taskKey[Unit](
    "Task to run using the release version but before publishing (e.g. generate documentation)")
}

object MavenCentralPlugin extends AutoPlugin {
  override def requires = Sonatype && ReleasePlugin

  import ReleasePlugin.autoImport._

  val autoImport = MavenCentralKeys
  import MavenCentralKeys._

  override def projectSettings: Seq[Setting[_]] = Seq(
    gitProjectName := name.value,
    developerHomePageUrl := s"https://github.com/${gitUserName.value}/${gitProjectName.value}",
    sonatypeCredentials := Path.userHome / ".ivy2" / "sonatype.txt",
    credentials ++= creds(sonatypeCredentials.value),
    pomExtra := SbtGit.gitPom(gitProjectName.value,
                              gitUserName.value,
                              developerName.value,
                              developerHomePageUrl.value),
    publishTo := Option(Opts.resolver.sonatypeStaging),
    publishArtifact in Test := false,
    beforePublish := {},
    releasePublishArtifactsAction := PgpKeys.publishSigned.value,
    releaseProcess := Seq[ReleaseStep](
      checkSnapshotDependencies,
      inquireVersions,
      runTest,
      setReleaseVersion,
      releaseStepTask(beforePublish),
      commitReleaseVersion,
      tagRelease,
      publishArtifacts, // : ReleaseStep, checks whether `publishTo` is properly set up
      setNextVersion,
      commitNextVersion,
      releaseStepCommand("sonatypeReleaseAll"),
      pushChanges
    ),
    resolvers += "Sonatype releases" at "https://oss.sonatype.org/content/repositories/releases/"
  )

  private def creds(file: File): Seq[DirectCredentials] =
    toSeq(Credentials.loadCredentials(file))

  def toSeq[A, B](either: Either[A, B]) =
    either.fold(_ => Seq.empty, value => Seq(value))
}
