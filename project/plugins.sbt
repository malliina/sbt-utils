scalaVersion := "2.12.14"

Seq(
  "org.xerial.sbt" % "sbt-sonatype" % "3.9.7",
  "com.github.gseitz" % "sbt-release" % "1.0.13",
  "com.jsuereth" % "sbt-pgp" % "2.1.1",
  "org.scalameta" % "sbt-mdoc" % "2.2.20"
//  "org.scala-sbt" % "sbt-maven-resolver" % "0.1.0"
) map addSbtPlugin
