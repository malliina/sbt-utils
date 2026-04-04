crossScalaVersions := Seq("3.4.2", "2.12.20")

libraryDependencies ++= Seq(
  "org.slf4j" % "slf4j-api" % "2.0.17",
  "com.malliina" %% "primitives" % "6.13.0",
  "commons-codec" % "commons-codec" % "1.21.0"
)

releaseCrossBuild := true
