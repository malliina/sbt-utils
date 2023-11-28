inThisBuild(
  Seq(
    scalaVersion := "3.3.1",
    organization := "com.malliina",
    version := "0.0.1"
  )
)

val shared = project.in(file("shared"))

val client = project
  .in(file("client"))
  .enablePlugins(RollupPlugin, NodeJsPlugin)
  .disablePlugins(RevolverPlugin)

val server = project
  .in(file("server"))
  .enablePlugins(ServerPlugin)
  .settings(
    clientProject := client,
    dependentModule := shared
  )
