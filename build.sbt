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
    scalaVersion := "2.12.18",
    licenses += ("MIT", url("https://opensource.org/licenses/MIT")),
    libraryDependencies ++= Seq(
      "org.scalameta" %% "munit" % "0.7.29" % Test
    ),
    testFrameworks += new TestFramework("munit.Framework")
  )
)

val pluginSettings = Seq(
  "com.github.sbt" % "sbt-pgp" % "2.2.1",
  "com.github.sbt" % "sbt-release" % "1.1.0"
) map addSbtPlugin

val docs = project
  .in(file("mdoc"))
  .settings(
    organization := "com.malliina",
    scalaVersion := "2.12.18",
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

val common = Project("common-build", file("common"))
  .settings(baseSettings)

val mavenPlugin = Project("sbt-utils-maven", file("maven"))
  .settings(commonSettings)
  .settings(
    addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "3.9.21")
  )

val nodePlugin = Project("sbt-nodejs", file("node-plugin"))
  .settings(commonSettings)

val fileTreePlugin = Project("sbt-filetree", file("filetree"))
  .settings(commonSettings)
  .settings(
    libraryDependencies += "org.scalameta" %% "scalafmt-dynamic" % "3.7.15"
  )

val bundlerPlugin = Project("sbt-bundler", file("bundler"))
  .settings(commonSettings)
  .settings(
    Seq(
      "ch.epfl.scala" % "sbt-scalajs-bundler" % "0.21.1",
      "com.malliina" % "live-reload" % "0.5.0"
    ) map addSbtPlugin
  )

val revolverRollupPlugin = Project("sbt-revolver-rollup", file("rollup"))
  .dependsOn(common, fileTreePlugin)
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq("generic", "parser").map { m =>
      "io.circe" %% s"circe-$m" % "0.14.6"
    },
    Seq(
      "com.malliina" % "live-reload" % "0.5.0",
      "org.scala-js" % "sbt-scalajs" % "1.14.0",
      "com.eed3si9n" % "sbt-buildinfo" % "0.11.0"
    ) map addSbtPlugin
  )

val dockerBundlerPlugin = Project("sbt-docker-bundler", file("docker-bundler"))
  .dependsOn(bundlerPlugin)
  .settings(commonSettings)
  .settings(
    addSbtPlugin("com.github.sbt" % "sbt-native-packager" % "1.9.16")
  )

val codeArtifactPlugin = Project("sbt-codeartifact", file("codeartifact"))
  .settings(commonSettings)
  .settings(
    libraryDependencies += "software.amazon.awssdk" % "codeartifact" % "2.21.10"
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
