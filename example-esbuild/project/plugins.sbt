scalaVersion := "2.12.20"

val rollup = ProjectRef(file("../.."), "sbt-revolver-rollup")
val node = ProjectRef(file("../.."), "sbt-nodejs")
val common = ProjectRef(file("../.."), "common-build")

val root = project.in(file(".")).dependsOn(common, node, rollup)

Seq(
  "com.malliina" % "live-reload" % "0.6.0",
  "org.scalameta" % "sbt-scalafmt" % "2.5.4",
  "com.eed3si9n" % "sbt-buildinfo" % "0.13.1"
) map addSbtPlugin
