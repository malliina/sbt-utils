package com.malliina.sbtutils

import com.typesafe.sbt.pgp.PgpKeys
import sbt.Keys._
import sbt._
import sbtrelease.ReleasePlugin
import sbtrelease.ReleaseStateTransformations._

object BintrayReleasePlugin extends AutoPlugin {
  override def requires = ReleasePlugin
  import ReleasePlugin.autoImport._

  val autoImport = ReleasePlugin.autoImport

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
      commitReleaseVersion,
      tagRelease,
      publishArtifacts, // : ReleaseStep, checks whether `publishTo` is properly set up
      setNextVersion,
      commitNextVersion,
      pushChanges // : ReleaseStep, also checks that an upstream branch is properly configured
    ),
    resolvers += "Sonatype releases" at "https://oss.sonatype.org/content/repositories/releases/",
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.5" % Test
  )
}