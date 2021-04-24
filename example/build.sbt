val client = project
  .in(file("client"))
  .enablePlugins(ClientPlugin)

val server = project
  .in(file("server"))
  .enablePlugins(ServerPlugin)
  .settings(
    clientProject := client
  )
