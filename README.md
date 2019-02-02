# sbt-utils

This SBT plugin helps publish GitHub projects to Sonatype and Bintray. It uses
[sbt-sonatype](https://github.com/xerial/sbt-sonatype) and fills in the POM XML
required for Maven central sync on the user's behalf. The user must provide
a couple of values to correctly populate the XML, see *Usage*.

## Installation

    addSbtPlugin("com.malliina" % "sbt-utils" % "0.9.1")

## Usage

To publish to Sonatype, add the following settings to your project:

    com.malliina.sbtutils.SbtUtils.mavenSettings ++ Seq(
      gitUserName := "My GitHub Username Here",
      developerName := "My Name Here"
    )
    
Type *release* to publish the artifacts. You need credentials to publish.

The following keys are available:

    > sbtUtilsHelp
    [info] sbtUtilsHelp             Shows help
    [info] gitUserName              Git username
    [info] developerName            Developer name
    [info] sonatypeCredentials      Path to sonatype credentials, defaults to ~/.ivy2/sonatype.txt
    [info] gitProjectName           Git project name
    [info] developerHomePageUrl     Developer home page URL
    [success] Total time: 0 s, completed 16.3.2014 20:56:25
    >

The generated POM XML declares a license of https://opensource.org/licenses/MIT.
