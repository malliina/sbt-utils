crossScalaVersions := Seq("3.4.2", "2.12.20")

libraryDependencies ++= Seq(
  "org.slf4j" % "slf4j-api" % "2.0.16",
  "com.malliina" %% "primitives" % "3.7.4",
  "commons-codec" % "commons-codec" % "1.17.2"
)

releaseCrossBuild := true
