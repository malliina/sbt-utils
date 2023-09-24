Seq(
  "ch.epfl.scala" % "sbt-scalajs-bundler" % "0.21.1",
  "org.scala-js" % "sbt-scalajs" % "1.13.2",
  "io.spray" % "sbt-revolver" % "0.9.1"
).map(addSbtPlugin)

lazy val bundler = ProjectRef(file("../.."), "sbt-bundler")
lazy val root = project.in(file(".")).dependsOn(bundler)
