scalaVersion := "2.12.20"

lazy val commonRef = RootProject(file("../common"))

lazy val root = project
  .in(file("."))
  .dependsOn(commonRef)
  .settings(
    libraryDependencies ++= Seq(
      "com.malliina" %% "primitives" % "3.7.7",
      "commons-codec" % "commons-codec" % "1.18.0"
    ),
    Seq(
      "org.xerial.sbt" % "sbt-sonatype" % "3.12.2",
      "com.github.sbt" % "sbt-release" % "1.4.0",
      "com.github.sbt" % "sbt-pgp" % "2.3.1",
      "org.scalameta" % "sbt-mdoc" % "2.6.5",
      "org.scalameta" % "sbt-scalafmt" % "2.5.4"
    ) map addSbtPlugin
  )
