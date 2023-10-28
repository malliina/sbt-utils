val shared = project.in(file("shared"))

val client = project
  .in(file("client"))
  .enablePlugins(RollupPlugin)
  .disablePlugins(RevolverPlugin)

val server = project
  .in(file("server"))
  .enablePlugins(ServerPlugin)
  .settings(
    clientProject := client,
    dependentModule := shared
  )
