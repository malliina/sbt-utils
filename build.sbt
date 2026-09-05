import com.jsuereth.sbtpgp.PgpKeys
import sbt.Keys.localStaging
import sbtrelease.ReleaseStateTransformations.*

import scala.sys.process.Process

// Uses Def.taskIf which is available only in 1.4.x
ThisBuild / pluginCrossBuild / sbtVersion := "2.0.3"

val tagReleaseProcess = settingKey[Seq[ReleaseStep]]("Tags and pushes a releasable version")
val updateDocs = taskKey[Unit]("Updates README.md")

inThisBuild(
  Seq(
    organization := "com.malliina",
    licenses += ("MIT", uri("https://opensource.org/licenses/MIT")),
    scalaVersion := "3.8.4",
    libraryDependencies ++= Seq(
      "org.scalameta" %% "munit" % versions.munit % Test
    )
  )
)

ThisBuild / commands += Command.command("releaseArtifacts") { state =>
  val extracted = Project.extract(state)
  val ciState = extracted.appendWithoutSession(
    Seq(
      releasePublishArtifactsAction := PgpKeys.publishSigned.value,
      releaseProcess := Seq[ReleaseStep](
        checkSnapshotDependencies,
        runTest,
        releaseStepCommandAndRemaining("+publishSigned"),
        releaseStepCommand("sonaRelease")
      )
    ),
    state
  )
  Command.process(
    "release cross with-defaults",
    ciState,
    str => sLog.value.error(s"Failed to parse command '$str'.")
  )
}

Global / pgpPassphrase := sys.env
  .get("PGP_PASSPHRASE")
  .orElse {
    val file = Path.userHome / ".sbt" / ".pgp"
    if (file.exists()) Option(IO.read(file)) else None
  }
  .map(_.toCharArray)

val pluginSettings = Seq(
  "com.github.sbt" % "sbt-pgp" % versions.sbtPgp,
  "com.github.sbt" % "sbt-release" % versions.sbtRelease
) map addSbtPlugin

val docs = project
  .in(file("mdoc"))
  .settings(
    organization := "com.malliina",
    publish / skip := true,
    mdocVariables := Map("VERSION" -> version.value),
    mdocOut := target.value / "docs",
    mdocExtraArguments += "--no-link-hygiene",
    updateDocs := Def.uncached {
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
  publishTo := localStaging.value
)

val commonSettings = pluginSettings ++ baseSettings ++ Seq(
  sbtPlugin := true
)

val common = Project("common-build", file("common"))
  .settings(baseSettings)

val mavenPlugin = Project("sbt-utils-maven", file("maven"))
  .settings(commonSettings)

val nodePlugin = Project("sbt-nodejs", file("node-plugin"))
  .settings(commonSettings)

val fileTreePlugin = Project("sbt-filetree", file("filetree"))
  .settings(commonSettings)
  .settings(
    libraryDependencies += ("org.scalameta" %% "scalafmt-dynamic" % versions.scalaFmt)
      .exclude("org.scala-lang.modules", "scala-xml_2.13")
  )

val liveReloadPlugin = Project("sbt-live-reload", file("live-reload"))
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq("ember-server", "dsl").map { m =>
      "org.http4s" %% s"http4s-$m" % "0.23.23"
    } ++ Seq(
      "io.circe" %% "circe-generic" % "0.14.16"
    ),
    addSbtPlugin("com.indoorvivants" % "sbt-revolver" % "0.11.2")
  )

val netlify = project
  .in(file("netlify"))
  .settings(baseSettings)
  .settings(
    libraryDependencies ++= Seq(
      "org.slf4j" % "slf4j-api" % versions.slf4j,
      "co.fs2" %% "fs2-io" % versions.fs2,
      "com.malliina" %% "okclient-io" % versions.primitives,
      "commons-codec" % "commons-codec" % versions.commonsCodec
    )
  )

val revolverRollupPlugin = Project("sbt-revolver-rollup", file("rollup"))
  .dependsOn(common, fileTreePlugin, nodePlugin, netlify, liveReloadPlugin)
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq("generic", "parser").map { m =>
      "io.circe" %% s"circe-$m" % versions.circe
    } ++ Seq(
      "org.scala-js" %% "scalajs-linker" % versions.scalaJs
    ),
    Seq(
      "org.scala-js" % "sbt-scalajs" % versions.scalaJs,
      "com.eed3si9n" % "sbt-buildinfo" % versions.sbtBuildInfo,
      "org.portable-scala" % "sbt-scalajs-crossproject" % versions.scalaJsCross,
      "com.github.sbt" % "sbt-native-packager" % versions.nativePackager
    ) map addSbtPlugin
  )

val codeArtifactPlugin = Project("sbt-codeartifact", file("codeartifact"))
  .settings(commonSettings)
  .settings(
    libraryDependencies += "software.amazon.awssdk" % "codeartifact" % versions.codeArtifact
  )

val sbtUtils = Project("sbt-utils", file("."))
  .aggregate(
    liveReloadPlugin,
    mavenPlugin,
    nodePlugin,
    fileTreePlugin,
    revolverRollupPlugin,
    codeArtifactPlugin,
    docs
  )
  .settings(releaseSettings)
  .settings(
    publish / skip := true,
    publishArtifact := false,
    publish := {},
    publishLocal := {}
  )

Global / onChangedBuildSource := ReloadOnSourceChanges
