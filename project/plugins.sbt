scalaVersion := "2.12.15"

Seq(
  "org.xerial.sbt" % "sbt-sonatype" % "3.9.10",
  "com.github.sbt" % "sbt-release" % "1.1.0",
  "com.jsuereth" % "sbt-pgp" % "2.1.1",
  "org.scalameta" % "sbt-mdoc" % "2.2.23"
) map addSbtPlugin
