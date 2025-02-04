inThisBuild(
  Seq(
    scalaVersion := "3.6.2",
    organization := "com.malliina",
    version := "0.0.1"
  )
)

val shared = project.in(file("shared"))

val client = project
  .in(file("client"))
  .enablePlugins(RollupPlugin, NodeJsPlugin, LoggingPlugin)
  .disablePlugins(RevolverPlugin)
  .settings(
    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom" % "2.8.0"
    )
  )

val server = project
  .in(file("server"))
  .enablePlugins(ServerPlugin)
  .settings(
    clientProject := client,
    dependentModule := shared,
    buildInfoPackage := "com.malliina.server",
    libraryDependencies ++=
      Seq("ember-server", "ember-client", "dsl", "circe").map { m =>
        "org.http4s" %% s"http4s-$m" % "0.23.30"
      } ++ Seq(
        "ch.qos.logback" % "logback-classic" % "1.5.16",
        "com.lihaoyi" %% "scalatags" % "0.13.1"
      )
  )
