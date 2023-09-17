package com.malliina.rollup

import sbt.Logger

import java.nio.file.Path
import scala.sys.process.Process

object IO {
  val isWindows = sys.props("os.name").toLowerCase().contains("win")
  val cmdPrefix = if (isWindows) Seq("cmd", "/c") else Nil

  def canonical(cmd: Seq[String]): Seq[String] = cmdPrefix ++ cmd

  def runProcessSync(command: Seq[String], cwd: Path, log: Logger): Unit = {
    val rc = runProcess(command, cwd, log)
    if (rc != 0) {
      throw new Exception(s"${command.mkString(" ")} failed with $rc")
    }
  }

  def runProcess(command: Seq[String], cwd: Path, log: Logger): Int = {
    val actualCommand = canonical(command)
    val cmdString = actualCommand.mkString(" ")
    log.info(s"Running '$cmdString' in $cwd...")
    Process(actualCommand, cwd.toFile).run(log).exitValue()
  }
}
