Seq(
  "ch.epfl.scala" % "sbt-scalajs-bundler" % "0.21.1",
  "org.scala-js" % "sbt-scalajs" % "1.14.0",
  "io.spray" % "sbt-revolver" % "0.10.0"
).map(addSbtPlugin)

val rollup = ProjectRef(file("../.."), "sbt-revolver-rollup")
val root = project.in(file(".")).dependsOn(rollup)
