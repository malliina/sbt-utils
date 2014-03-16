# sbt-utils #

This SBT plugin helps publish GitHub projects to Sonatype repositories. It uses
[sbt-sonatype](https://github.com/xerial/sbt-sonatype) and fills in the POM XML
required for Maven central sync on the user's behalf. The user must instead provide
a couple of values to correctly populate the XML, see Usage.

## Installation ##

    addSbtPlugin("com.github.malliina" % "sbt-utils" % "0.0.2")

## Usage ##

Add the following settings to your project:

```
com.mle.sbtutils.SbtUtils.publishSettings ++ Seq(
  gitUserName := "malliina",
  developerName := "Michael Skogberg"
)
```

The following keys are available:

```
> sbtUtilsHelp
[info] sbtUtilsHelp             Shows help
[info] gitUserName              Git username
[info] developerName            Developer name
[info] sonatypeCredentials      Path to sonatype credentials, defaults to ~/.ivy2/sonatype.txt
[info] gitProjectName           Git project name
[info] developerHomePageUrl     Developer home page URL
[success] Total time: 0 s, completed 16.3.2014 20:56:25
>
```

The generated POM XML declares a license of http://www.opensource.org/licenses/BSD-3-Clause.