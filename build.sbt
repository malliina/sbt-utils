import com.typesafe.sbt.pgp.PgpKeys
import sbtrelease.ReleaseStateTransformations._

lazy val sbtUtils = Project("sbt-utils", file("."))

organization := "com.malliina"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.4" % Test
sbtPlugin := true
scalaVersion := "2.12.4"
bintrayOrganization := None
bintrayRepository := "sbt-plugins"
publishMavenStyle := false
licenses += ("MIT", url("http://opensource.org/licenses/MIT"))

releasePublishArtifactsAction := PgpKeys.publishSigned.value

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
)

Seq(
  "com.jsuereth" % "sbt-pgp" % "1.1.0",
  "org.xerial.sbt" % "sbt-sonatype" % "2.0",
  "com.github.gseitz" % "sbt-release" % "1.0.6",
  "org.foundweekends" % "sbt-bintray" % "0.5.1"
) map addSbtPlugin
