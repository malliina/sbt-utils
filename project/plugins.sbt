scalaVersion := "2.12.19"

lazy val commonRef = RootProject(file("../common"))

lazy val root = project
  .in(file("."))
  .dependsOn(commonRef)
  .settings(
    libraryDependencies ++= Seq(
      "com.malliina" %% "primitives" % "3.7.3",
      "commons-codec" % "commons-codec" % "1.17.1"
    ),
    Seq(
      "org.xerial.sbt" % "sbt-sonatype" % "3.11.1",
      "com.github.sbt" % "sbt-release" % "1.4.0",
      "com.github.sbt" % "sbt-pgp" % "2.2.1",
      "org.scalameta" % "sbt-mdoc" % "2.5.4",
      "org.scalameta" % "sbt-scalafmt" % "2.5.2"
    ) map addSbtPlugin
  )
