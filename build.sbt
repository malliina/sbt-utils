import com.typesafe.sbt.pgp.PgpKeys
import sbtrelease.ReleaseStateTransformations._

val baseSettings = Seq(
  organization := "com.malliina",
  scalaVersion := "2.12.8",
  licenses += ("MIT", url("http://opensource.org/licenses/MIT"))
)

val pluginSettings = Seq(
  "com.jsuereth" % "sbt-pgp" % "1.1.2",
  "com.github.gseitz" % "sbt-release" % "1.0.11"
) map addSbtPlugin

val commonSettings = baseSettings ++ pluginSettings ++ Seq(
  libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.7" % Test,
  sbtPlugin := true,
  bintrayOrganization := None,
  bintrayRepository := "sbt-plugins",
  publishMavenStyle := false,
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
  )
)

pgpPassphrase in ThisBuild := sys.env.get("PGP_PASSPHRASE").orElse {
  val file = Path.userHome / ".sbt" / ".pgp"
  if (file.exists()) Option(IO.read(file)) else None
}.map(_.toCharArray)

val sbtUtilsMaven = Project("sbt-utils-maven", file("maven"))
  .settings(commonSettings)
  .settings(
    addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "2.5")
  )

val sbtUtilsBintray = Project("sbt-utils-bintray", file("bintray"))
  .settings(commonSettings)
  .settings(
    addSbtPlugin("org.foundweekends" % "sbt-bintray" % "0.5.4")
  )

val sbtUtils = Project("sbt-utils", file("."))
  .aggregate(sbtUtilsMaven, sbtUtilsBintray)
  .settings(
    skip in publish := true,
    publishArtifact := false,
    packagedArtifacts := Map.empty,
    publish := {},
    publishLocal := {}
  )
