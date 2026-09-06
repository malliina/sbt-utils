lazy val commonRef = RootProject(file("../common"))

lazy val root = project
  .in(file("."))
  .dependsOn(commonRef)
  .settings(
    libraryDependencies ++= Seq(
      "com.malliina" %% "primitives" % "6.15.4",
      "commons-codec" % "commons-codec" % "1.22.1"
    ),
    Seq(
      "com.github.sbt" % "sbt-release" % "1.5.0",
      "com.github.sbt" % "sbt-pgp" % "2.3.1",
      "org.scalameta" % "sbt-mdoc" % "2.9.0",
      "org.scalameta" % "sbt-scalafmt" % "2.6.1"
    ) map addSbtPlugin
  )
