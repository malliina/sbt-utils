import com.jsuereth.sbtpgp.PgpKeys
import sbtrelease.ReleaseStateTransformations.*
import scala.sys.process.Process

// Uses Def.taskIf which is available only in 1.4.x
ThisBuild / pluginCrossBuild / sbtVersion := "1.4.9"

val versions = new {
  val liveReload = "0.6.0"
  val scala212 = "2.12.20"
  val scalaJS = "1.18.2"
  val nativePackager = "1.11.0"
}

val tagReleaseProcess = settingKey[Seq[ReleaseStep]]("Tags and pushes a releasable version")
val updateDocs = taskKey[Unit]("Updates README.md")

inThisBuild(
  Seq(
    organization := "com.malliina",
    scalaVersion := versions.scala212,
    licenses += ("MIT", url("https://opensource.org/licenses/MIT")),
    libraryDependencies ++= Seq(
      "org.scalameta" %% "munit" % "1.1.0" % Test
    )
  )
)

ThisBuild / commands += Command.command("releaseArtifacts") { state =>
  val extracted = Project extract state
  val ciState = extracted.appendWithoutSession(
    Seq(
      releasePublishArtifactsAction := PgpKeys.publishSigned.value,
      releaseProcess := Seq[ReleaseStep](
        checkSnapshotDependencies,
        runTest,
        releaseStepCommandAndRemaining("+publishSigned"),
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

val pluginSettings = Seq(
  "com.github.sbt" % "sbt-pgp" % "2.3.1",
  "com.github.sbt" % "sbt-release" % "1.4.0"
) map addSbtPlugin

val docs = project
  .in(file("mdoc"))
  .settings(
    organization := "com.malliina",
    scalaVersion := "2.12.20",
    crossScalaVersions -= "2.13.16",
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

val baseSettings = releaseSettings ++ Seq(
  pomExtra := SbtGit.gitPom(
    "sbt-utils",
    "malliina",
    "Michael Skogberg",
    "https://github.com/malliina/sbt-utils"
  ),
  publishTo := Option(Opts.resolver.sonatypeStaging)
)

val commonSettings = pluginSettings ++ baseSettings ++ Seq(
  sbtPlugin := true
)

val common = Project("common-build", file("common"))
  .settings(baseSettings)

val mavenPlugin = Project("sbt-utils-maven", file("maven"))
  .settings(commonSettings)
  .settings(
    addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "3.12.2")
  )

val nodePlugin = Project("sbt-nodejs", file("node-plugin"))
  .settings(commonSettings)

val fileTreePlugin = Project("sbt-filetree", file("filetree"))
  .settings(commonSettings)
  .settings(
    libraryDependencies += "org.scalameta" %% "scalafmt-dynamic" % "3.9.4"
  )

val bundlerPlugin = Project("sbt-bundler", file("bundler"))
  .settings(commonSettings)
  .settings(
    Seq(
      "ch.epfl.scala" % "sbt-scalajs-bundler" % "0.21.1",
      "com.malliina" % "live-reload" % versions.liveReload
    ) map addSbtPlugin
  )

val netlify = project
  .in(file("netlify"))
  .settings(baseSettings)
  .settings(
    crossScalaVersions := Seq("3.4.2", versions.scala212),
    libraryDependencies ++= Seq(
      "org.slf4j" % "slf4j-api" % "2.0.17",
      "co.fs2" %% "fs2-io" % "3.11.0",
      "com.malliina" %% "okclient-io" % "3.7.7",
      "commons-codec" % "commons-codec" % "1.18.0"
    )
  )

val revolverRollupPlugin = Project("sbt-revolver-rollup", file("rollup"))
  .dependsOn(common, fileTreePlugin, nodePlugin, netlify)
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq("generic", "parser").map { m =>
      "io.circe" %% s"circe-$m" % "0.14.11"
    } ++ Seq(
      "org.scala-js" %% "scalajs-linker" % versions.scalaJS
    ),
    Seq(
      "com.malliina" % "live-reload" % versions.liveReload,
      "org.scala-js" % "sbt-scalajs" % versions.scalaJS,
      "com.eed3si9n" % "sbt-buildinfo" % "0.13.1",
      "org.portable-scala" % "sbt-scalajs-crossproject" % "1.3.2",
      "com.github.sbt" % "sbt-native-packager" % versions.nativePackager
    ) map addSbtPlugin
  )

val dockerBundlerPlugin = Project("sbt-docker-bundler", file("docker-bundler"))
  .dependsOn(bundlerPlugin)
  .settings(commonSettings)
  .settings(
    addSbtPlugin("com.github.sbt" % "sbt-native-packager" % versions.nativePackager)
  )

val codeArtifactPlugin = Project("sbt-codeartifact", file("codeartifact"))
  .settings(commonSettings)
  .settings(
    libraryDependencies += "software.amazon.awssdk" % "codeartifact" % "2.31.11"
  )

val sbtUtils = Project("sbt-utils", file("."))
  .aggregate(
    mavenPlugin,
    nodePlugin,
    fileTreePlugin,
    bundlerPlugin,
    revolverRollupPlugin,
    dockerBundlerPlugin,
    codeArtifactPlugin,
    docs
  )
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
