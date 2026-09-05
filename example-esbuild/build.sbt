import com.malliina.rollup.CommonKeys.isProd
import sbt.*
import sbt.Keys.*

inThisBuild(
  Seq(
    scalaVersion := "3.9.0",
    organization := "com.malliina",
    version := "0.0.1"
  )
)

val a = taskKey[Unit]("a")
val b = taskKey[Unit]("b")
val c = taskKey[Unit]("c")

val shared = project.in(file("shared"))

val frontend = project
  .in(file("frontend"))
  .enablePlugins(EsbuildPlugin, NodeJsPlugin)
  .disablePlugins(RevolverPlugin)
  .settings(
    cwd := EsbuildPlugin.autoImport.npmRoot.value,
    libraryDependencies ++= Seq(
      "org.scala-js" %% "scalajs-dom" % versions.scalaJsDom
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
      ),
    a := Def.uncached(streams.value.log.info("a")),
    b := streams.value.log.info("b"),
    c := streams.value.log.info("c"),
    a := a.dependsOn(b, c).value
  )

Global / onChangedBuildSource := ReloadOnSourceChanges
