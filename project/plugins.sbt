scalaVersion := "2.12.13"

Seq(
  "org.xerial.sbt" % "sbt-sonatype" % "3.9.7",
  "com.github.gseitz" % "sbt-release" % "1.0.13",
  "com.jsuereth" % "sbt-pgp" % "2.1.1",
  "org.scalameta" % "sbt-mdoc" % "2.2.18"
) map addSbtPlugin
