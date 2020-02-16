import com.jsuereth.sbtpgp.PgpKeys
import sbtrelease.ReleaseStateTransformations._
import scala.sys.process.Process

ThisBuild / pluginCrossBuild / sbtVersion := "1.2.8"

val tagReleaseProcess = settingKey[Seq[ReleaseStep]]("Tags and pushes a releasable version")
val updateDocs = taskKey[Unit]("Updates README.md")

val baseSettings = Seq(
  organization := "com.malliina",
  scalaVersion := "2.12.10",
  licenses += ("MIT", url("http://opensource.org/licenses/MIT"))
)

val pluginSettings = Seq(
  "com.jsuereth" % "sbt-pgp" % "2.0.1",
  "com.github.gseitz" % "sbt-release" % "1.0.13"
) map addSbtPlugin

val docs = project
  .in(file("mdoc"))
  .settings(
    organization := "com.malliina",
    scalaVersion := "2.12.10",
    crossScalaVersions -= "2.13.1",
    skip.in(publish) := true,
    mdocVariables := Map("VERSION" -> version.value),
    mdocOut := (baseDirectory in ThisBuild).value,
    updateDocs := {
      val log = streams.value.log
      val outFile = mdocOut.value
      IO.relativize((baseDirectory in ThisBuild).value, outFile)
        .getOrElse(sys.error(s"Strange directory: $outFile"))
      val addStatus = Process(s"git add $outFile").run(log).exitValue()
      if (addStatus != 0) {
        sys.error(s"Unexpected status code $addStatus for git commit.")
      }
    },
    updateDocs := updateDocs.dependsOn(mdoc.toTask("")).value
  )
  .enablePlugins(MdocPlugin)

val releaseSettings = Seq(
  publishMavenStyle := true,
  releaseProcess := Seq[ReleaseStep](
    checkSnapshotDependencies,
    inquireVersions,
    runTest,
    setReleaseVersion,
    releaseStepTask(updateDocs in docs),
    commitReleaseVersion,
    tagRelease,
    setNextVersion,
    commitNextVersion,
    pushChanges
  )
)

val commonSettings = baseSettings ++ pluginSettings ++ releaseSettings ++ Seq(
  libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.8" % Test,
  sbtPlugin := true,
  pomExtra := SbtGit.gitPom(
    "sbt-utils",
    "malliina",
    "Michael Skogberg",
    "https://github.com/malliina/sbt-utils"
  ),
  publishTo := Option(Opts.resolver.sonatypeStaging)
)

commands in ThisBuild += Command.command("releaseArtifacts") { state =>
  val extracted = Project extract state
  val ciState = extracted.appendWithoutSession(
    Seq(
      releasePublishArtifactsAction := PgpKeys.publishSigned.value,
      releaseProcess := Seq[ReleaseStep](
        checkSnapshotDependencies,
        runTest,
        publishArtifacts
      )
    ),
    state
  )
  Command.process("release with-defaults", ciState)
}

pgpPassphrase in Global := sys.env
  .get("PGP_PASSPHRASE")
  .orElse {
    val file = Path.userHome / ".sbt" / ".pgp"
    if (file.exists()) Option(IO.read(file)) else None
  }
  .map(_.toCharArray)

val sbtUtilsMaven = Project("sbt-utils-maven", file("maven"))
  .settings(commonSettings)
  .settings(
    addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "3.8.1")
  )

val sbtUtilsBintray = Project("sbt-utils-bintray", file("bintray"))
  .settings(commonSettings)
  .settings(
    addSbtPlugin("org.foundweekends" % "sbt-bintray" % "0.5.4")
  )

val nodePlugin = Project("sbt-nodejs", file("node-plugin"))
  .settings(commonSettings)
  .settings(
    addSbtPlugin("ch.epfl.scala" % "sbt-web-scalajs-bundler" % "0.16.0")
  )

val sbtUtils = Project("sbt-utils", file("."))
  .aggregate(sbtUtilsMaven, sbtUtilsBintray, nodePlugin, docs)
  .settings(releaseSettings)
  .settings(
    skip in publish := true,
    publishArtifact := false,
    packagedArtifacts := Map.empty,
    publish := {},
    publishLocal := {},
    sonatypeProfileName := "com.malliina"
  )

Global / onChangedBuildSource := ReloadOnSourceChanges
