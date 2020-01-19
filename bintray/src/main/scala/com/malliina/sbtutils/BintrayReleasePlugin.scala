package com.malliina.sbtutils

import com.jsuereth.sbtpgp.SbtPgp.autoImport.pgpPassphrase
import com.jsuereth.sbtpgp.PgpKeys
import sbt.Keys._
import sbt._
import sbtrelease.ReleasePlugin
import sbtrelease.ReleasePlugin.autoImport.ReleaseStep
import sbtrelease.ReleaseStateTransformations._

object BintrayReleaseKeys {
  val beforeCommitRelease = taskKey[Unit]("Task to run before the release version is committed")
  val beforePublish = taskKey[Unit](
    "Task to run using the release version but before publishing (e.g. generate documentation)"
  )
  val afterPublish = taskKey[Unit]("Task to run after artifacts have been published")
  val tagReleaseProcess = settingKey[Seq[ReleaseStep]]("Tags and pushes a releasable version")
  val fullReleaseProcess = settingKey[Seq[ReleaseStep]]("Runs the entire release process")
}

object BintrayReleasePlugin extends AutoPlugin {
  override def requires = ReleasePlugin
  import ReleasePlugin.autoImport._

  val autoImport = BintrayReleaseKeys
  import BintrayReleaseKeys._

  override def buildSettings: Seq[Setting[_]] = Seq(
    pgpPassphrase := sys.env
      .get("PGP_PASSPHRASE")
      .orElse {
        val file = Path.userHome / ".sbt" / ".pgp"
        if (file.exists()) Option(IO.read(file)) else None
      }
      .map(_.toCharArray())
  )

  override def globalSettings: Seq[Def.Setting[_]] = Seq(
    publishArtifact in Test := false,
    publishMavenStyle := false,
    beforeCommitRelease := {},
    beforePublish := {},
    afterPublish := {},
    commands += Command.command("releaseArtifacts") { state =>
      val extracted = Project extract state
      val ciState = extracted.appendWithoutSession(
        Seq(
          releasePublishArtifactsAction := PgpKeys.publishSigned.value,
          releaseProcess := Seq[ReleaseStep](
            checkSnapshotDependencies,
            runTest,
            publishArtifacts,
            releaseStepTask(afterPublish)
          )
        ),
        state
      )
      Command.process("release with-defaults", ciState)
    }
  )

  override def projectSettings: Seq[Setting[_]] = Seq(
    sbtPlugin := true,
    scalaVersion := "2.12.10",
    licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
    beforePublish := {},
    afterPublish := {},
    releasePublishArtifactsAction := PgpKeys.publishSigned.value,
    fullReleaseProcess := Seq[ReleaseStep](
      checkSnapshotDependencies,
      inquireVersions,
      runTest,
      setReleaseVersion,
      releaseStepTask(beforePublish),
      commitReleaseVersion,
      tagRelease,
      publishArtifacts,
      setNextVersion,
      commitNextVersion,
      releaseStepTask(afterPublish),
      pushChanges
    ),
    tagReleaseProcess := Seq[ReleaseStep](
      checkSnapshotDependencies,
      inquireVersions,
      runTest,
      setReleaseVersion,
      releaseStepTask(beforeCommitRelease),
      commitReleaseVersion,
      tagRelease,
      setNextVersion,
      commitNextVersion,
      pushChanges
    ),
    releaseProcess := tagReleaseProcess.value,
    releaseCrossBuild := true
  )
}
