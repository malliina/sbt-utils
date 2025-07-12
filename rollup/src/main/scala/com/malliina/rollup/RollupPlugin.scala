package com.malliina.rollup

import com.malliina.build.FileIO
import com.malliina.nodejs.NodeJsPlugin
import com.malliina.rollup.CommonKeys.assetsPrefix
import io.circe.parser.parse
import io.circe.syntax.EncoderOps
import org.apache.ivy.util.ChecksumHelper
import org.scalajs.linker.interface.Report
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport.*
import org.scalajs.sbtplugin.{LinkerImpl, ScalaJSPlugin, Stage}
import sbt.Keys.*
import sbt.nio.Keys.fileInputs
import sbt.{IO => _, *}

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import scala.jdk.CollectionConverters.asScalaBufferConverter

object RollupPlugin extends AutoPlugin {
  override def requires: Plugins = ScalaJSPlugin && NodeJsPlugin
  val utf8 = StandardCharsets.UTF_8
  val sha1 = "sha1"

  object autoImport {
    val build = CommonKeys.build
    val prepareRollup = taskKey[Path]("Prepares rollup")
    val assetsRoot = CommonKeys.assetsRoot
    val npmRoot = settingKey[Path]("Working dir for npm commands")
    val urlOptions = settingKey[Seq[UrlOption]]("URL options for postcss-url")
    val resourceLockFile = settingKey[Path]("Path to saved package-lock.json")
    val entryPointFile = settingKey[Path]("Entrypoint file")
    val entryPointJsFile = settingKey[Path]("Entrypoint JS file")
    val libraryRollupFile = settingKey[Path]("Rollup config for libraries only")
    val importedModules = taskKey[List[String]]("Links stuff")
    val link = taskKey[Unit]("Links")
  }
  import autoImport.*

  override val projectSettings: Seq[Def.Setting[?]] =
    stageSettings(Stage.FastOpt) ++
      stageSettings(Stage.FullOpt) ++
      localDevSettings(fastLinkJS) ++
      localDevSettings(fullLinkJS) ++
      Seq(
        npmRoot := target.value.toPath,
        resourceLockFile := (Compile / resourceDirectory).value.toPath.resolve("package-lock.json"),
        scalaJSUseMainModuleInitializer := true,
        scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.CommonJSModule) },
        assetsRoot := target.value.toPath / "assets",
        assetsPrefix := "assets",
        build := Def.settingDyn {
          val stageTask = scalaJSStage.value match {
            case Stage.FastOpt => fastLinkJS
            case Stage.FullOpt => fullLinkJS
          }
          stageTask / build
        }.value
      )

  private def localDevSettings(stage: TaskKey[Attributed[Report]]) = Seq(
    stage / build := (stage / build).dependsOn(stage / link).value,
    stage / entryPointFile := (crossTarget.value / "entrypoints.txt").toPath,
    stage / entryPointJsFile := (crossTarget.value / "entrypoints.js").toPath,
    stage / libraryRollupFile := (crossTarget.value / "library.rollup.config.js").toPath,
    Compile / stage / scalaJSLinker := {
      val out = (stage / entryPointFile).value
      val config = (stage / scalaJSLinkerConfig).value
      val linkerImpl = (stage / scalaJSLinkerImpl).value
      val box = (Compile / stage / scalaJSLinkerBox).value
      box.ensure {
        linkerImpl.asInstanceOf[ForwardingLinker].bundlerLinker(config, out)
      }
    },
    stage / importedModules := {
      val lines = Files.readAllLines((stage / entryPointFile).value)
      lines.asScala.toList
    },
    stage / importedModules := (stage / importedModules).dependsOn(Compile / stage).value,
    stage / link := {
      val appName = name.value
      writeLoaderScript("version", assetsRoot.value / s"$appName-loader.js")
    }
  )

  private def writeEntryPoint(modules: Seq[String], to: Path): Path = {
    val map = modules.map { module =>
      s""""$module": require("$module")"""
    }.mkString(",\n")
    val content =
      s"""
        |module.exports = {
        |  "require": (function(x0) {
        |    return {
        |      $map
        |    }[x0]
        |  })
        |}
        |""".stripMargin
    FileIO.writeIfChanged(content, to)
    to
  }

  private def writeLoaderScript(bundleName: String, to: Path): Boolean = {
    val content =
      s"""
         |var exports = window;
         |exports.require = window["$bundleName"].require;
    """.stripMargin
    FileIO.writeIfChanged(content, to)
  }

  override val globalSettings: Seq[Setting[?]] = Seq(
    commands += Command.args("mode", "<mode>") { (state, args) =>
      val newStage = args.toList match {
        case h :: Nil =>
          h match {
            case "prod" => FullOptStage
            case "dev"  => FastOptStage
            case other  => sys.error(s"Invalid mode: '$other'.")
          }
        case other => sys.error("Specify either dev or prod as the only argument.")
      }
      state.appendWithoutSession(
        Seq(
          Global / scalaJSStage := newStage
        ),
        state
      )
    },
    scalaJSLinkerImpl / fullClasspath := {
      val s = streams.value
      val log = s.log
      val retrieveDir = s.cacheDirectory / "scalajs-bundler-linker"
      val lm = (scalaJSLinkerImpl / dependencyResolution).value
      val dependencies = Vector(
        "org.scala-js" % "scalajs-linker_2.12" % scalaJSVersion
      )
      val dummyModuleID =
        "com.malliina" % "scalajs-rollup-linker-and-scalajs-linker_2.12" % s"$scalaJSVersion"
      val moduleDescriptor =
        lm.moduleDescriptor(dummyModuleID, dependencies, scalaModuleInfo = None)
      lm.retrieve(moduleDescriptor, retrieveDir, log)
        .fold(w => throw w.resolveException, Attributed.blankSeq(_))
    },
    scalaJSLinkerImpl := {
      val cp = (scalaJSLinkerImpl / fullClasspath).value
      scalaJSLinkerImplBox.value.ensure {
        new ForwardingLinker(LinkerImpl.reflect(Attributed.data(cp)))
      }
    }
  )

  private def stageSettings(stage: Stage): Seq[Setting[?]] = {
    val stageTaskOutput = stage match {
      case Stage.FastOpt => fastLinkJSOutput
      case Stage.FullOpt => fullLinkJSOutput
    }
    val stageTask = stage match {
      case Stage.FastOpt => fastLinkJS
      case Stage.FullOpt => fullLinkJS
    }
    val isProd = stage == Stage.FullOpt
    Seq(
      stageTask / urlOptions := UrlOption.defaults,
      stageTask / prepareRollup := {
        val jsDir = (Compile / stageTaskOutput).value.toPath
        val jsFilename = "main.js"
        FileIO.copyIfChanged(jsDir.resolve(jsFilename), assetsRoot.value.resolve(jsFilename))
        val sourceMap = jsDir.resolve(s"$jsFilename.map")
        if (Files.exists(sourceMap)) {
          FileIO.copyIfChanged(sourceMap, assetsRoot.value.resolve(s"$jsFilename.map"))
        }
        val jsFile = (Compile / stageTask).value.data.publicModules
          .find(_.moduleID == "main")
          .getOrElse(
            sys.error("Main module not found. Expected a module ID named 'main' in task output.")
          )
          .jsFileName
        val root = npmRoot.value
        val mainJs = root.relativize(jsDir) / jsFile
        val modules = (stageTask / importedModules).value
        val libraryEntryPointJs = (stageTask / entryPointJsFile).value
        val rollupEntry =
          if (isProd) mainJs
          else writeEntryPoint(modules, libraryEntryPointJs)
        val rollup = root / "scalajs.rollup.config.js"
        makeRollupConfig(
          rollupEntry,
          assetsRoot.value,
          rollup,
          (stageTask / urlOptions).value,
          isProd
        )
        writePackageJsonIfChanged((Compile / resourceDirectory).value, root)
        val tsFiles =
          Seq("rollup.config.ts", "rollup-extract-css.ts", "rollup-sourcemaps.ts", "tsconfig.json")
        tsFiles.foreach { name =>
          FileIO.writeIfChanged(res(name), root.resolve(name))
        }
        val lockFile = resourceLockFile.value
        val lockFileDest = root / "package-lock.json"
        if (Files.exists(lockFile)) {
          FileIO.copyIfChanged(lockFile, lockFileDest)
        }
        jsDir
      },
      stageTask / build / fileInputs ++=
        (Compile / sourceDirectories).value.map(f => f.toGlob / ** / "*.scala") ++
          (Compile / resourceDirectories).value.map(f => f.toGlob / ** / *) ++
          Seq(baseDirectory.value.toGlob / "*.ts") ++
          Seq(baseDirectory.value / "package.json").map(_.toGlob),
      stageTask / build := {
        val log = streams.value.log
        val cwd = npmRoot.value
        val packageJson = cwd / "package.json"
        val cacheFile = cwd / "package.json.sha1"
        val checksum = computeChecksum(packageJson)
        if (
          Files.exists(cacheFile) && Files
            .readAllLines(cacheFile, utf8)
            .asScala
            .headOption
            .contains(checksum)
        ) {
          npmRunBuild(cwd, log)
        } else {
          FileIO.writeIfChanged(checksum, cacheFile)
          if (isProd) npmCi(cwd, log)
          else {
            npmInstall(cwd, log)
            val lockFile = resourceLockFile.value
            val newestLockFile = cwd / "package-lock.json"
            if (Files.exists(newestLockFile)) {
              FileIO.copyIfChanged(newestLockFile, lockFile)
            }
          }
          npmRunBuild(cwd, log)
        }
      },
      stageTask / build := (stageTask / build).dependsOn(stageTask / prepareRollup).value,
      stageTask / build := Def.taskIf {
        val hasChanges = build.inputFileChanges.hasChanges || writePackageJsonIfChanged(
          (Compile / resourceDirectory).value,
          npmRoot.value
        )
        if (hasChanges) {
          (stageTask / build).value
        } else {
          Def.task(()).value
        }
      }.value
    )
  }

  def writePackageJsonIfChanged(resDir: File, destDir: Path): Boolean = {
    val userPackageJson = resDir / "package.json"
    val inbuilt = json(res("package.json"))
    val packageJson =
      if (userPackageJson.exists())
        inbuilt.deepMerge(jsonFile(userPackageJson))
      else
        inbuilt
    FileIO.writeIfChanged(packageJson.spaces2SortKeys, destDir.resolve("package.json"))
  }

  def npmRunBuild(cwd: Path, log: Logger) =
    process(Seq("npm", "run", "build"), cwd, log)

  def npmInstall(cwd: Path, log: Logger) =
    process(Seq("npm", "install"), cwd, log)

  def npmCi(cwd: Path, log: Logger) =
    process(Seq("npm", "ci"), cwd, log)

  def process(commands: Seq[String], cwd: Path, log: Logger) =
    IO.runProcessSync(commands, cwd, log)

  def computeChecksum(file: Path) = ChecksumHelper.computeAsString(file.toFile, sha1)

  def makeRollupConfig(
    input: Path,
    outputDir: Path,
    rollup: Path,
    urlOptions: Seq[UrlOption],
    isProd: Boolean
  ): Path = {
    val json = urlOptions.asJson.noSpaces
    val isProdStr = if (isProd) "true" else "false"
    val content = s"""
      |// Generated at build time
      |export const production = $isProdStr
      |export const outputDir = "${IO.render(outputDir)}"
      |export const urlOptions = JSON.parse('$json')
      |export const scalajs = {
      |  input: { frontend: "${IO.render(input)}" },
      |  output: {
      |    dir: outputDir,
      |    format: "iife",
      |    sourcemap: !production,
      |    name: "version"
      |  }
      |}""".stripMargin.trim
    FileIO.writeIfChanged(content, rollup)
    rollup
  }

  def jsonFile(f: File) = json(sbt.io.IO.read(f, utf8))

  def json(str: String) = parse(str).fold(err => fail(err.message), identity)

  def res(name: String): String = {
    val path = s"com/malliina/rollup/$name"
    Option(getClass.getClassLoader.getResourceAsStream(path))
      .map(inStream => FileIO.using(inStream)(in => sbt.io.IO.readStream(in, utf8)))
      .getOrElse(fail(s"Resource not found: '$path'."))
  }

  def fail(message: String) = sys.error(message)
}
