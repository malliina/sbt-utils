import com.malliina.rollup.CommonKeys.isProd

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

val shared = project.in(file("shared"))

val frontend = project
  .in(file("frontend"))
  .enablePlugins(EsbuildPlugin)
  .disablePlugins(RevolverPlugin)
  .settings(
    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom" % versions.scalaJsDom
    )
  )

val server = project
  .in(file("server"))
  .enablePlugins(ServerPlugin)
  .settings(
    clientProject := frontend,
    dependentModule := shared,
    buildInfoPackage := "com.malliina.server",
    libraryDependencies ++=
      Seq("ember-server", "ember-client", "dsl", "circe").map { m =>
        "org.http4s" %% s"http4s-$m" % versions.http4s
      } ++ Seq(
        "ch.qos.logback" % "logback-classic" % versions.logback,
        "com.lihaoyi" %% "scalatags" % versions.scalatags
      )
  )

Global / onChangedBuildSource := ReloadOnSourceChanges
