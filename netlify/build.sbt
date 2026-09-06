scalaVersion := "3.8.4"

libraryDependencies ++= Seq(
  "org.slf4j" % "slf4j-api" % "2.0.19",
  "co.fs2" %% "fs2-io" % "3.13.0",
  "com.malliina" %% "okclient-io" % "6.15.4",
  "commons-codec" % "commons-codec" % "1.22.1",
  "org.scalameta" %% "munit" % "1.3.6" % Test,
  "org.typelevel" %% "munit-cats-effect-3" % "1.0.7" % Test
)

Global / onChangedBuildSource := ReloadOnSourceChanges
