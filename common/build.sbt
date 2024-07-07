crossScalaVersions := Seq("3.2.2", "2.12.19")

libraryDependencies ++= Seq(
  "org.slf4j" % "slf4j-api" % "2.0.12",
  "com.malliina" %% "primitives" % "3.6.0",
  "commons-codec" % "commons-codec" % "1.17.0"
)

releaseCrossBuild := true
