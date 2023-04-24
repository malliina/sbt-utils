package com.malliina.rollup

import _root_.io.circe.parser.parse
import _root_.io.circe.syntax.EncoderOps
import com.malliina.build.FileIO
import org.apache.ivy.util.ChecksumHelper
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport.*
import org.scalajs.sbtplugin.{ScalaJSPlugin, Stage}
import sbt.*
import sbt.Keys.*
import sbt.nio.Keys.fileInputs

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import scala.jdk.CollectionConverters.asScalaBufferConverter
import scala.sys.process.{Process, ProcessLogger}

object RollupPlugin extends AutoPlugin {
  override def requires: Plugins = ScalaJSPlugin
  val utf8 = StandardCharsets.UTF_8
  val sha1 = "sha1"

  object autoImport {
    val build = CommonKeys.build
    val prepareRollup = taskKey[Path]("Prepares rollup")
    val assetsRoot = CommonKeys.assetsRoot
    val assetsPrefix = settingKey[String]("I don't know what this is")
    val npmRoot = settingKey[Path]("Working dir for npm commands")
    val urlOptions = settingKey[Seq[UrlOption]]("URL options for postcss-url")
    val resourceLockFile = settingKey[Path]("Path to saved package-lock.json")
  }
  import autoImport.*

  override val projectSettings: Seq[Def.Setting[?]] =
    stageSettings(Stage.FastOpt) ++
      stageSettings(Stage.FullOpt) ++
      Seq(
        npmRoot := target.value.toPath,
        resourceLockFile := (Compile / resourceDirectory).value.toPath.resolve("package-lock.json"),
        scalaJSUseMainModuleInitializer := true,
        scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.CommonJSModule) },
        assetsRoot := target.value.toPath / "assets",
        assetsPrefix := "assets",
        build := {
          Def.settingDyn {
            val stageTask = scalaJSStage.value match {
              case Stage.FastOpt => fastLinkJS
              case Stage.FullOpt => fullLinkJS
            }
            stageTask / build
          }
        }.value
      )

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
        val log = streams.value.log
        val jsDir = (Compile / stageTaskOutput).value.toPath
        val jsFile = (Compile / stageTask).value.data.publicModules
          .find(_.moduleID == "main")
          .getOrElse(
            sys.error("Main module not found. Expected a module ID named 'main' in task output.")
          )
          .jsFileName
        val root = npmRoot.value
        val mainJs = root.relativize(jsDir) / jsFile
        log.info(s"Built $mainJs with prod $isProd.")
        val rollup = npmRoot.value / "scalajs.rollup.config.js"
        makeRollupConfig(mainJs, assetsRoot.value, rollup, (stageTask / urlOptions).value, isProd)
        val resDir = (Compile / resourceDirectory).value
        val userPackageJson = resDir / "package.json"
        val inbuilt = json(res("package.json"))
        val packageJson =
          if (userPackageJson.exists())
            inbuilt.deepMerge(jsonFile(userPackageJson))
          else
            inbuilt
        val targetPath = npmRoot.value
        val dest = targetPath / "package.json"
        val tsFiles =
          Seq("rollup.config.ts", "rollup-extract-css.ts", "rollup-sourcemaps.ts", "tsconfig.json")
        tsFiles.foreach { name =>
          FileIO.writeIfChanged(res(name), targetPath.resolve(name))
        }
        FileIO.writeIfChanged(packageJson.spaces2SortKeys, dest)
        val lockFile = resourceLockFile.value
        val lockFileDest = npmRoot.value / "package-lock.json"
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
        val cacheFile = npmRoot.value / "package.json.sha1"
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
            val newestLockFile = npmRoot.value / "package-lock.json"
            if (Files.exists(newestLockFile)) {
              FileIO.copyIfChanged(newestLockFile, lockFile)
            }
          }
          npmRunBuild(cwd, log)
        }
      },
      stageTask / build := (stageTask / build).dependsOn(stageTask / prepareRollup).value,
      stageTask / build := Def.taskIf {
        val hasChanges = build.inputFileChanges.hasChanges
        if (hasChanges) {
          (stageTask / build).value
        } else {
          Def.task(()).value
        }
      }.value
    )
  }

  def npmRunBuild(cwd: Path, log: ProcessLogger) =
    process(Seq("npm", "run", "build"), cwd, log)

  def npmInstall(cwd: Path, log: ProcessLogger) =
    process(Seq("npm", "install"), cwd, log)

  def npmCi(cwd: Path, log: ProcessLogger) =
    process(Seq("npm", "ci"), cwd, log)

  def process(commands: Seq[String], cwd: Path, log: ProcessLogger) = {
    val canon = canonical(commands)
    val cmdStr = canon.mkString(" ")
    log.out(s"Running '$cmdStr' from '$cwd'...")
    val exitValue = Process(canon, cwd.toFile).run(log).exitValue()
    if (exitValue != 0) {
      fail(s"Command '$cmdStr' failed with exit value $exitValue.")
    }
  }

  def canonical(cmd: Seq[String]): Seq[String] = {
    val isWindows = sys.props("os.name").toLowerCase().contains("win")
    val cmdPrefix = if (isWindows) Seq("cmd", "/c") else Nil
    cmdPrefix ++ cmd
  }

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
      |export const outputDir = "$outputDir"
      |export const urlOptions = JSON.parse('$json')
      |export const scalajs = {
      |  input: { frontend: "$input" },
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

  def jsonFile(f: File) = json(IO.read(f, utf8))

  def json(str: String) = parse(str).fold(err => fail(err.message), identity)

  def res(name: String): String = {
    val path = s"com/malliina/rollup/$name"
    Option(getClass.getClassLoader.getResourceAsStream(path))
      .map(inStream => FileIO.using(inStream)(in => IO.readStream(in, utf8)))
      .getOrElse(fail(s"Resource not found: '$path'."))
  }

  def fail(message: String) = sys.error(message)
}
