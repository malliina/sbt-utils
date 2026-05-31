scalaVersion := "2.12.21"

lazy val commonRef = RootProject(file("../common"))

lazy val root = project
  .in(file("."))
  .dependsOn(commonRef)
  .settings(
    libraryDependencies ++= Seq(
      "com.malliina" %% "primitives" % "6.14.3",
      "commons-codec" % "commons-codec" % "1.22.0"
    ),
    Seq(
      "org.xerial.sbt" % "sbt-sonatype" % "3.12.2",
      "com.github.sbt" % "sbt-release" % "1.5.0",
      "com.github.sbt" % "sbt-pgp" % "2.3.1",
      "org.scalameta" % "sbt-mdoc" % "2.9.0",
      "org.scalameta" % "sbt-scalafmt" % "2.6.1"
    ) map addSbtPlugin
  )
