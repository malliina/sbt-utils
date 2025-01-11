[![Build Status](https://github.com/malliina/sbt-utils/workflows/Test/badge.svg)](https://github.com/malliina/sbt-utils/actions)

# sbt-utils

A repository of sbt plugins that I find useful.

- sbt-bundler for integrating servers using sbt-revolver with clients built with Scala.js and scalajs-bundler
- sbt-utils-maven for publishing GitHub projects to [Maven Central](https://search.maven.org/)
- sbt-nodejs for working with Scala and Node.js projects
- sbt-filetree creates a data structure of your files

The Maven Central plugin populates the required POM XML and delegates publishing to 
[sbt-sonatype](https://github.com/xerial/sbt-sonatype). The user must provide a couple of values to correctly populate 
the POM XML, see *Usage*.

## Installation

To use sbt-revolver-rollup:

    addSbtPlugin("com.malliina" % "sbt-revolver-rollup" % "1.6.43")

To use sbt-bundler:

    addSbtPlugin("com.malliina" % "sbt-bundler" % "1.6.43")

To publish to Maven Central:

    addSbtPlugin("com.malliina" % "sbt-utils-maven" % "1.6.43")
    
The Node.js plugin:

    addSbtPlugin("com.malliina" % "sbt-nodejs" % "1.6.43")

To use sbt-filetree:

    addSbtPlugin("com.malliina" % "sbt-filetree" % "1.6.43")

## Usage

### sbt-revolver-rollup

Static site generation with hot reload support via sbt-revolver. Configure the frontend (Scala.js) and site generator.

#### Frontend

Enable the frontend plugin:

```scala
val frontend = project
  .in(file("frontend"))
  .enablePlugins(RollupPlugin)
```

Create and edit `package.json` in

    src/main/resources/package.json

The following rollup inputs are available:

    src/main/resources/css/app.js

and

    src/main/resources/css/fonts.js

#### Generator

Enable the generator plugin and connect the frontend module:

```scala
val generator = project
  .in(file("generator"))
  .enablePlugins(GeneratorPlugin)
  .settings(
    scalajsProject := frontend,
    hashPackage := "com.malliina.assets",
  )
```

Assets generated by the frontend will be available in `com.malliina.assets.HashedAssets`.

Implement your site generation function and call it from the main method of the project. Write the site to 
directory `BuildInfo.siteDir`. I recommend [Scalatags](https://com-lihaoyi.github.io/scalatags/) for HTML generation.

Run `sbt ~build` and navigate to `http://localhost:10101` to view the site.

### sbt-bundler

Define your project:

```scala
// Scala.js project
val client = project
  .in(file("client"))
  .enablePlugins(ClientPlugin)

// Backend restarted with sbt-revolver
val server = project
  .in(file("server"))
  .enablePlugins(ServerPlugin)
  .settings(
    clientProject := client
  )
```

To run:

    sbt ~start

The `start` command will fingerprint assets generated by scalajs-bundler (via webpack) and make them available
as resources to the server module. Any changes to either the client or server will rebuild and reload. See 
[example/build.sbt](example/build.sbt) for a minimal example.

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

### Node.js

Plugin `NodeJsPlugin` lets you run npm commands from the sbt shell.

    val myApp = project.in(file("."))
      .enablePlugins(NodeJsPlugin)

Then run e.g.

    front ncu

Set sbt setting `cwd` to your frontend working directory. (Typically, the directory containing `package.json`.)
By default, it is `baseDirectory`.

### sbt-filetree

This SBT plugin generates a Scala source file that represents the
directory structure of a given file system directory.

For example, given a directory with the following structure:

    base/
      file1.jpg
      file2.jpg
      sub/
        file3.jpg

The plugin generates (roughly) the following Scala object:

    object AppAssets {
      def file1_jpg: String = "file1.jpg"
      def file2_jpg: String = "file2.jpg"
      object sub {
        def file3_jpg: String = "sub/file3.jpg"
      }
    }

Now you can refer to a given file path without writing the string literal by hand:

    AppAssets.sub.file3_jpg // returns "sub/file3.jpg"

Furthermore, if the file is deleted or moved from the file system, any code that
references the file will no longer compile.

#### Installation

Add the following settings in `plugins.sbt`:

    addSbtPlugin("com.malliina" % "sbt-filetree" % "1.6.43")

Enable `FileTreePlugin` in your project:

    val myProject = Project("demo", file("."))
      .enablePlugins(com.malliina.sbt.filetree.FileTreePlugin)

Specify the source directories for the file tree traversal and corresponding destination objects to write:

    import com.malliina.sbt.filetree.DirMap
    fileTreeSources += DirMap(baseDirectory.value / "appfiles", "com.malliina.filetree.AppFiles")

Now when you `compile`, the build generates source code that defines an object `com.malliina.filetree.AppFiles`,
where each member is a file or directory under `baseDirectory / appfiles`.

By default, each file path is represented as a `String`. You can supply a function as a third paramater to a
`DirMap` that transforms each file path:

    DirMap(baseDirectory.value / "appfiles", "com.malliina.filetree.AppFiles", "com.malliina.Code.transform")

Where `transform` is a unary function of type `String => T`.
