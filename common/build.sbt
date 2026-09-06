libraryDependencies ++= Seq(
  "org.slf4j" % "slf4j-api" % "2.0.19",
  "com.malliina" %% "primitives" % "6.15.4",
  "commons-codec" % "commons-codec" % "1.22.1",
  "ch.qos.logback" % s"logback-classic" % "1.6.3"
)

releaseCrossBuild := true
