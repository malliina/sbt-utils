scalaVersion := "2.12.10"

Seq(
  "org.xerial.sbt" % "sbt-sonatype" % "3.9.2",
  "com.github.gseitz" % "sbt-release" % "1.0.13",
  "com.jsuereth" % "sbt-pgp" % "2.0.1",
  "org.scalameta" % "sbt-mdoc" % "2.2.0"
) map addSbtPlugin
