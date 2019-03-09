# sbt-utils

This repo contains two SBT plugins that make it easier to publish projects on GitHub to 
[Maven Central](https://search.maven.org/) and [Bintray](https://bintray.com/).
 
For Maven Central, [sbt-sonatype](https://github.com/xerial/sbt-sonatype) is used, furthermore the
 POM XML required for Maven Central sync is filled on the user's behalf. The user must provide
a couple of values to correctly populate the XML, see *Usage*.

## Installation

To publish to Maven Central:

    addSbtPlugin("com.malliina" % "sbt-utils-maven" % "0.11.0")
    
To publish to Bintray: 

    addSbtPlugin("com.malliina" % "sbt-utils-bintray" % "0.11.0")

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

### Which one should I use?

Maven Central is stable and well-known, however I have not been able to successfully publish SBT 
plugins there, therefore I use Bintray for SBT plugin artifacts and Maven Central for 
everything else.
