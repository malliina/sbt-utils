inThisBuild(
  Seq(
    scalaVersion := "3.7.1",
    organization := "com.malliina",
    version := "0.0.1"
  )
)

val versions = new {
  val http4s = "0.23.30"
  val logback = "1.5.18"
  val scalaJsDom = "2.8.0"
  val scalatags = "0.13.1"
}

val start = taskKey[Unit]("Starts the project")

val client = project
  .in(file("client"))
  .enablePlugins(ScalaJSEsbuildPlugin)
  .disablePlugins(RevolverPlugin)
  .settings(
    scalaJSUseMainModuleInitializer := true,
    scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.CommonJSModule) },
    esbuildResourcesDirectory := (Compile / resourceDirectory).value,
    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom" % versions.scalaJsDom
    )
  )

val server = project
  .in(file("server"))
  .enablePlugins(BuildInfoPlugin, LiveRevolverPlugin)
  .settings(
    Compile / compile := (Compile / compile).dependsOn(client / Compile / esbuildBundle).value,
    buildInfoPackage := "com.malliina.server",
    buildInfoKeys ++= Seq[BuildInfoKey](
      "assetsDir" -> (client / Compile / esbuildStage / crossTarget).value,
      "publicFolder" -> "todo",
      "isProd" -> false,
      "mode" -> "dev",
      "gitHash" -> "todo"
    ),
    libraryDependencies ++=
      Seq("ember-server", "ember-client", "dsl", "circe").map { m =>
        "org.http4s" %% s"http4s-$m" % versions.http4s
      } ++ Seq(
        "ch.qos.logback" % "logback-classic" % versions.logback,
        "com.lihaoyi" %% "scalatags" % versions.scalatags
      ),
    start := reStart.toTask(" ").value,
    refreshBrowsers := refreshBrowsers.triggeredBy(start).value
  )

Global / onChangedBuildSource := ReloadOnSourceChanges
