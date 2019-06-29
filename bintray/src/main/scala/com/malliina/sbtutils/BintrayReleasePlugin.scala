package com.malliina.sbtutils

import com.typesafe.sbt.SbtPgp.autoImportImpl.pgpPassphrase
import com.typesafe.sbt.pgp.PgpKeys
import sbt.Keys._
import sbt._
import sbtrelease.ReleasePlugin
import sbtrelease.ReleasePlugin.autoImport.ReleaseStep
import sbtrelease.ReleaseStateTransformations._

object BintrayReleaseKeys {
  val beforePublish = taskKey[Unit](
    "Task to run using the release version but before publishing (e.g. generate documentation)")
  val tagReleaseProcess = settingKey[Seq[ReleaseStep]]("Tags and pushes a releasable version")
}

object BintrayReleasePlugin extends AutoPlugin {
  override def requires = ReleasePlugin
  import ReleasePlugin.autoImport._

  val autoImport = BintrayReleaseKeys
  import BintrayReleaseKeys.{beforePublish, tagReleaseProcess}

  override def buildSettings: Seq[Setting[_]] = Seq(
    pgpPassphrase := sys.env.get("PGP_PASSPHRASE").orElse {
      val file = Path.userHome / ".sbt" / ".pgp"
      if (file.exists()) Option(IO.read(file)) else None
    }.map(_.toCharArray())
  )

  override def globalSettings: Seq[Def.Setting[_]] = Seq(
    publishArtifact in Test := false,
    publishMavenStyle := false,
    commands += Command.command("releaseArtifacts") { state =>
      val extracted = Project extract state
      val ciState = extracted.appendWithoutSession(Seq(
        releasePublishArtifactsAction := PgpKeys.publishSigned.value,
        releaseProcess := Seq[ReleaseStep](
          checkSnapshotDependencies,
          runTest,
          publishArtifacts
        )), state)
      Command.process("release with-defaults", ciState)
    }
  )

  override def projectSettings: Seq[Setting[_]] = Seq(
    sbtPlugin := true,
    scalaVersion := "2.12.8",
    licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
    releasePublishArtifactsAction := PgpKeys.publishSigned.value,
    releaseProcess := Seq[ReleaseStep](
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
    )
  )
}
