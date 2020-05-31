[![Build Status](https://github.com/malliina/sbt-utils/workflows/Test/badge.svg)](https://github.com/malliina/sbt-utils/actions)

# sbt-utils

A repository of sbt plugins that I find useful.

- sbt-utils-maven for publishing GitHub projects to [Maven Central](https://search.maven.org/)
- sbt-utils-bintray for publishing to [Bintray](https://bintray.com/)
- sbt-nodejs for working with Scala and Node.js projects

The Maven Central plugin populates the required POM XML and delegates publishing to 
[sbt-sonatype](https://github.com/xerial/sbt-sonatype). The user must provide a couple of values to correctly populate 
the POM XML, see *Usage*.

## Installation

To publish to Maven Central:

    addSbtPlugin("com.malliina" % "sbt-utils-maven" % "1.0.0")
    
To publish to Bintray:

    addSbtPlugin("com.malliina" % "sbt-utils-bintray" % "1.0.0")
    
The Node.js plugin:

    addSbtPlugin("com.malliina" % "sbt-nodejs" % "1.0.0")

## Usage

Use either Maven Central or Bintray depending on where you want to publish your artifacts.

### Maven Central

To publish to Maven Central, enable the `MavenCentralPlugin` SBT autoplugin for your project:

    val myLibrary = Project("my-library", file("."))
      .enablePlugins(MavenCentralPlugin)

Define the following SBT settings in order to populate the Maven POM XML correctly:

    gitUserName := "My GitHub Username Here",
    developerName := "My Name Here"
    
To publish the artifacts, run: 

    sbt release

You need credentials to publish.

The generated POM XML declares a license of https://opensource.org/licenses/MIT.

### Bintray

Enable the `BintrayReleasePlugin` autoplugin:

    val myLibrary = Project("my-library", file("."))
      .enablePlugins(BintrayReleasePlugin)
      
To publish the artifacts, run: 

    sbt release

### Node.js

Plugin `NodeJsPlugin` lets you run npm commands from the sbt shell.

    val myApp = project.in(file("."))
      .enablePlugins(NodeJsPlugin)

Then run e.g.

    front ncu
