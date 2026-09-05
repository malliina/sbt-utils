val rollup = ProjectRef(file("../.."), "sbt-revolver-rollup")
val node = ProjectRef(file("../.."), "sbt-nodejs")
val common = ProjectRef(file("../.."), "common-build")
val liveReload = ProjectRef(file("../.."), "sbt-live-reload")

val root = project.in(file(".")).dependsOn(common, node, rollup, liveReload)

Seq(
  "org.scalameta" % "sbt-scalafmt" % "2.6.1",
  "com.eed3si9n" % "sbt-buildinfo" % "0.13.1"
) map addSbtPlugin
