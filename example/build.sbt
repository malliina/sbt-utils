inThisBuild(
  Seq(
    scalaVersion := "3.3.1",
    organization := "com.malliina",
    version := "0.0.1",
    Compile / fastLinkJS / scalaJSLinkerConfig ~= { _.withSourceMap(false) },
    Compile / fastOptJS / scalaJSLinkerConfig ~= { _.withSourceMap(false) },
    Compile / fullLinkJS / scalaJSLinkerConfig ~= { _.withSourceMap(false) },
    Compile / fullOptJS / scalaJSLinkerConfig ~= { _.withSourceMap(false) },
  )
)

val shared = project.in(file("shared"))

val client = project
  .in(file("client"))
  .enablePlugins(RollupPlugin, NodeJsPlugin)
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
        "org.http4s" %% s"http4s-$m" % "0.23.25"
      } ++ Seq("classic", "core").map { m =>
        "ch.qos.logback" % s"logback-$m" % "1.4.14"
      } ++ Seq(
        "com.lihaoyi" %% "scalatags" % "0.12.0"
      )
  )
