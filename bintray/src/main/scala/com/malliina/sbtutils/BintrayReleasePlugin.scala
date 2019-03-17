package com.malliina.sbtutils

import com.typesafe.sbt.pgp.PgpKeys
import sbt.Keys._
import sbt._
import sbtrelease.ReleasePlugin
import sbtrelease.ReleaseStateTransformations._

object BintrayReleaseKeys {
  val beforePublish = taskKey[Unit](
    "Task to run using the release version but before publishing (e.g. generate documentation)")
}

object BintrayReleasePlugin extends AutoPlugin {
  override def requires = ReleasePlugin
  import ReleasePlugin.autoImport._

  val autoImport = BintrayReleaseKeys
  import BintrayReleaseKeys.beforePublish

  override def projectSettings: Seq[Setting[_]] = Seq(
    sbtPlugin := true,
    scalaVersion := "2.12.8",
    publishMavenStyle := false,
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
      publishArtifacts, // : ReleaseStep, checks whether `publishTo` is properly set up
      setNextVersion,
      commitNextVersion,
      pushChanges // : ReleaseStep, also checks that an upstream branch is properly configured
    ),
    resolvers += "Sonatype releases" at "https://oss.sonatype.org/content/repositories/releases/"
  )
}
