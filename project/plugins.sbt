scalaVersion := "2.12.20"

lazy val commonRef = RootProject(file("../common"))

lazy val root = project
  .in(file("."))
  .dependsOn(commonRef)
  .settings(
    libraryDependencies ++= Seq(
      "com.malliina" %% "primitives" % "6.13.0",
      "commons-codec" % "commons-codec" % "1.21.0"
    ),
    Seq(
      "org.xerial.sbt" % "sbt-sonatype" % "3.12.2",
      "com.github.sbt" % "sbt-release" % "1.4.0",
      "com.github.sbt" % "sbt-pgp" % "2.3.1",
      "org.scalameta" % "sbt-mdoc" % "2.7.2",
      "org.scalameta" % "sbt-scalafmt" % "2.5.5"
    ) map addSbtPlugin
  )
