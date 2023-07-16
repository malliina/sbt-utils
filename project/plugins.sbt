scalaVersion := "2.12.18"

lazy val commonRef = RootProject(file("../common"))

lazy val root = project
  .in(file("."))
  .dependsOn(commonRef)
  .settings(
    libraryDependencies ++= Seq(
      "com.malliina" %% "primitives" % "3.4.4",
      "commons-codec" % "commons-codec" % "1.16.0"
    ),
    Seq(
      "org.xerial.sbt" % "sbt-sonatype" % "3.9.21",
      "com.github.sbt" % "sbt-release" % "1.1.0",
      "com.github.sbt" % "sbt-pgp" % "2.2.1",
      "org.scalameta" % "sbt-mdoc" % "2.3.7",
      "org.scalameta" % "sbt-scalafmt" % "2.5.0"
    ) map addSbtPlugin
  )
