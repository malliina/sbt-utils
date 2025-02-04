crossScalaVersions := Seq("3.4.2", "2.12.20")

libraryDependencies ++= Seq(
  "org.slf4j" % "slf4j-api" % "2.0.16",
  "com.malliina" %% "primitives" % "3.7.7",
  "commons-codec" % "commons-codec" % "1.18.0"
)

releaseCrossBuild := true
