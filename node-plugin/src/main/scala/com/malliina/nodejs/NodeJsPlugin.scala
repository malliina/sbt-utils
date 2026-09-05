package com.malliina.nodejs

import sbt.Keys.*
import sbt.{IO as _, *}
import complete.DefaultParsers.*
import java.nio.file.Path

import scala.sys.process.{Process, ProcessLogger}

object NodeJsPlugin extends AutoPlugin:
  object autoImport:
    val checkNodeOnStartup = settingKey[Boolean]("When true, runs node version check on startup")
    val checkNode = taskKey[Unit]("Make sure the user uses the correct version of node.js")
    val failMode =
      settingKey[FailMode]("Whether to warn or fail hard when the node version is unsupported")
    val ncu = taskKey[Int]("Runs npm-check-updates")
    val front = inputKey[Int]("Runs the input as a command in the frontend working directory")
    val cwd = settingKey[Path]("The frontend working directory")
    val preferredNodeVersion = settingKey[String]("Preferred node version, e.g. '24'")
  import autoImport.*

  override val globalSettings: Seq[Def.Setting[?]] = Seq(
    preferredNodeVersion := "24",
    failMode := FailMode.Warn,
    checkNodeOnStartup := false,
    checkNode := Def.uncached(
      runNodeCheck(preferredNodeVersion.value, streams.value.log, failMode.value)
    ),
    Global / onLoad := (Global / onLoad).value andThen { state =>
      if checkNodeOnStartup.value then "checkNode" :: state
      else state
    }
  )

  override val projectSettings = Seq(
    cwd := target.value.toPath,
    ncu := front.toTask(s" npx npm-check-updates").value,
    front := {
      val log = streams.value.log
      val args: Seq[String] = spaceDelimited("<arg>").parsed
      val status = IO.runProcess(args, cwd.value.toFile, log)
      if status != 0 then log.error(s"Command '${args.mkString(" ")}' exited with status $status.")
      status
    }
  )

  def runNodeCheck(preferredVersion: String, log: ProcessLogger, failMode: FailMode): Unit =
    val nodeVersion = Process("node --version")
      .lazyLines(log)
      .toList
      .headOption
      .getOrElse(sys.error(s"Unable to resolve node version."))
    val validPrefixes = Seq(s"v$preferredVersion")
    if validPrefixes.exists(p => nodeVersion.startsWith(p)) then
      log.out(s"Using node $nodeVersion.")
    else
      val cmd = IO.canonical(Seq("nvm", "use", preferredVersion))
      log.out(
        s"Node $nodeVersion is unlikely to work. Trying to change version using '${cmd.mkString(" ")}'..."
      )
      try Process(cmd).run(log).exitValue()
      catch
        case _: Exception if failMode == FailMode.Warn =>
          log.err(s"Unable to change node version to '$preferredVersion' using nvm.")

sealed trait FailMode

object FailMode:
  object Warn extends FailMode
  object Fail extends FailMode
