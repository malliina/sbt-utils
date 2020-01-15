[![Build Status](https://github.com/malliina/sbt-utils/workflows/Test/badge.svg)](https://github.com/malliina/sbt-utils/actions)

# sbt-utils

A repository of sbt plugins that I find useful.

- sbt-utils-maven for publishing GitHub projects to [Maven Central](https://search.maven.org/)
- sbt-utils-bintray for publishing to [Bintray](https://bintray.com/)
- sbt-nodejs for working with Scala and Node.js projects

For Maven Central, [sbt-sonatype](https://github.com/xerial/sbt-sonatype) is used, furthermore the
POM XML required for Maven Central sync is filled on the user's behalf. The user must provide
a couple of values to correctly populate the XML, see *Usage*.

## Installation

To publish to Maven Central:

    addSbtPlugin("com.malliina" % "sbt-utils-maven" % "@VERSION@")
    
To publish to Bintray:

    addSbtPlugin("com.malliina" % "sbt-utils-bintray" % "@VERSION@")
    
The Node.js plugin:

    addSbtPlugin("com.malliina" % "sbt-nodejs" % "@VERSION@")

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

### Which one should I use?

Maven Central is stable and well-known, however I have not been able to successfully publish SBT 
plugins there, therefore I use Bintray for SBT plugin artifacts and Maven Central for 
everything else.
