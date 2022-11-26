import com.jsuereth.sbtpgp.PgpKeys
import sbtrelease.ReleaseStateTransformations._
import scala.sys.process.Process

// Uses Def.taskIf which is available only in 1.4.x
ThisBuild / pluginCrossBuild / sbtVersion := "1.4.9"

val tagReleaseProcess = settingKey[Seq[ReleaseStep]]("Tags and pushes a releasable version")
val updateDocs = taskKey[Unit]("Updates README.md")

inThisBuild(
  Seq(
    organization := "com.malliina",
    scalaVersion := "2.12.17",
    licenses += ("MIT", url("https://opensource.org/licenses/MIT"))
  )
)

val pluginSettings = Seq(
  "com.jsuereth" % "sbt-pgp" % "2.1.1",
  "com.github.sbt" % "sbt-release" % "1.1.0"
) map addSbtPlugin

val docs = project
  .in(file("mdoc"))
  .settings(
    organization := "com.malliina",
    scalaVersion := "2.12.17",
    crossScalaVersions -= "2.13.10",
    publish / skip := true,
    mdocVariables := Map("VERSION" -> version.value),
    mdocOut := target.value / "docs",
    mdocExtraArguments += "--no-link-hygiene",
    updateDocs := {
      val log = streams.value.log
      val outFile = mdocOut.value / "README.md"
      val rootReadme = (ThisBuild / baseDirectory).value / "README.md"
      IO.copyFile(outFile, rootReadme)
      val addStatus = Process(s"git add $rootReadme").run(log).exitValue()
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
    releaseStepTask(docs / updateDocs),
    commitReleaseVersion,
    tagRelease,
    setNextVersion,
    commitNextVersion,
    pushChanges
  )
)

val commonSettings = pluginSettings ++ releaseSettings ++ Seq(
  sbtPlugin := true,
  pomExtra := SbtGit.gitPom(
    "sbt-utils",
    "malliina",
    "Michael Skogberg",
    "https://github.com/malliina/sbt-utils"
  ),
  publishTo := Option(Opts.resolver.sonatypeStaging)
)

ThisBuild / commands += Command.command("releaseArtifacts") { state =>
  val extracted = Project extract state
  val ciState = extracted.appendWithoutSession(
    Seq(
      releasePublishArtifactsAction := PgpKeys.publishSigned.value,
      releaseProcess := Seq[ReleaseStep](
        checkSnapshotDependencies,
        runTest,
        publishArtifacts,
        releaseStepCommand("sonatypeReleaseAll")
      )
    ),
    state
  )
  Command.process("release cross with-defaults", ciState)
}

Global / pgpPassphrase := sys.env
  .get("PGP_PASSPHRASE")
  .orElse {
    val file = Path.userHome / ".sbt" / ".pgp"
    if (file.exists()) Option(IO.read(file)) else None
  }
  .map(_.toCharArray)

val mavenPlugin = Project("sbt-utils-maven", file("maven"))
  .settings(commonSettings)
  .settings(
    addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "3.9.15")
  )

val bundlerVersion = "0.21.1"

val nodePlugin = Project("sbt-nodejs", file("node-plugin"))
  .settings(commonSettings)
  .settings(
    addSbtPlugin("ch.epfl.scala" % "sbt-scalajs-bundler" % bundlerVersion)
  )

val bundlerPlugin = Project("sbt-bundler", file("bundler"))
  .settings(commonSettings)
  .settings(
    addSbtPlugin("ch.epfl.scala" % "sbt-scalajs-bundler" % bundlerVersion),
    addSbtPlugin("io.spray" % "sbt-revolver" % "0.9.1"),
    addSbtPlugin("com.malliina" % "live-reload" % "0.4.0")
  )

val dockerBundlerPlugin = Project("sbt-docker-bundler", file("docker-bundler"))
  .dependsOn(bundlerPlugin)
  .settings(commonSettings)
  .settings(
    addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.8.1")
  )

val codeArtifactPlugin = Project("sbt-codeartifact", file("codeartifact"))
  .settings(commonSettings)
  .settings(
    libraryDependencies += "software.amazon.awssdk" % "codeartifact" % "2.18.24"
  )

val sbtUtils = Project("sbt-utils", file("."))
  .aggregate(mavenPlugin, nodePlugin, bundlerPlugin, dockerBundlerPlugin, codeArtifactPlugin, docs)
  .settings(releaseSettings)
  .settings(
    publish / skip := true,
    publishArtifact := false,
    packagedArtifacts := Map.empty,
    publish := {},
    publishLocal := {},
    sonatypeProfileName := "com.malliina"
  )

Global / onChangedBuildSource := ReloadOnSourceChanges
