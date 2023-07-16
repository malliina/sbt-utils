crossScalaVersions := Seq("3.2.2", "2.12.18")

libraryDependencies ++= Seq(
  "org.slf4j" % "slf4j-api" % "2.0.5",
  "com.malliina" %% "primitives" % "3.4.4",
  "commons-codec" % "commons-codec" % "1.16.0"
)

releaseCrossBuild := true
