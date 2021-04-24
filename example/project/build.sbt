Seq(
  "ch.epfl.scala" % "sbt-scalajs-bundler" % "0.20.0",
  "org.scala-js" % "sbt-scalajs" % "1.5.0",
  "io.spray" % "sbt-revolver" % "0.9.1"
).map(addSbtPlugin)

lazy val bundler = ProjectRef(file("../.."), "sbt-bundler")
lazy val root = project.in(file(".")).dependsOn(bundler)
