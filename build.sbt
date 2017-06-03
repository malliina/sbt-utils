import com.malliina.sbtutils.SbtProjects

lazy val sbtUtils = SbtProjects.sbtPlugin("sbt-utils")

organization := "com.malliina"

Seq(
  "com.jsuereth" % "sbt-pgp" % "1.0.0",
  "org.xerial.sbt" % "sbt-sonatype" % "1.1",
  "com.github.gseitz" % "sbt-release" % "1.0.3",
  "me.lessis" % "bintray-sbt" % "0.2.1"
) map addSbtPlugin
