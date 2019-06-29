package com.malliina.sbtutils

import com.typesafe.sbt.SbtPgp.autoImport.pgpPassphrase
import com.typesafe.sbt.pgp.PgpKeys
import sbt.Keys._
import sbt._
import sbtrelease.ReleasePlugin
import sbtrelease.ReleasePlugin.autoImport.ReleaseStep
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
  val tagReleaseProcess = settingKey[Seq[ReleaseStep]]("Tags and pushes a releasable version")
}

object MavenCentralPlugin extends AutoPlugin {
  override def requires = Sonatype && ReleasePlugin

  import ReleasePlugin.autoImport._

  val autoImport = MavenCentralKeys
  import MavenCentralKeys._

  override def buildSettings: Seq[Setting[_]] = Seq(
    pgpPassphrase := sys.env.get("PGP_PASSPHRASE").orElse {
      val file = Path.userHome / ".sbt" / ".pgp"
      if (file.exists()) Option(IO.read(file)) else None
    }.map(_.toCharArray())
  )

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
    tagReleaseProcess := Seq[ReleaseStep](
      checkSnapshotDependencies,
      inquireVersions,
      runTest,
      setReleaseVersion,
      commitReleaseVersion,
      tagRelease,
      setNextVersion,
      commitNextVersion,
      pushChanges
    ),
    commands += Command.command("releaseArtifacts") { state =>
      val extracted = Project extract state
      val ciState = extracted.appendWithoutSession(Seq(releaseProcess := Seq[ReleaseStep](
        checkSnapshotDependencies,
        runTest,
        publishArtifacts,
        releaseStepCommand("sonatypeReleaseAll")
      )), state)
      Command.process("release with-defaults", ciState)
    }
  )

  private def creds(file: File): Seq[DirectCredentials] =
    toSeq(Credentials.loadCredentials(file))

  def toSeq[A, B](either: Either[A, B]) =
    either.fold(_ => Seq.empty, value => Seq(value))
}
