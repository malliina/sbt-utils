scalaVersion := "2.12.20"

Seq(
  "com.malliina" % "live-reload" % "0.6.0",
  "me.ptrdom" % "sbt-scalajs-esbuild" % "0.1.2",
  "org.scalameta" % "sbt-scalafmt" % "2.5.4",
  "com.eed3si9n" % "sbt-buildinfo" % "0.13.1"
) map addSbtPlugin
