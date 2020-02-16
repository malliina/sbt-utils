scalaVersion := "2.12.10"

Seq(
  "org.xerial.sbt" % "sbt-sonatype" % "3.8.1",
  "com.github.gseitz" % "sbt-release" % "1.0.13",
  "com.jsuereth" % "sbt-pgp" % "2.0.1",
  "org.scalameta" % "sbt-mdoc" % "1.3.2"
) map addSbtPlugin
