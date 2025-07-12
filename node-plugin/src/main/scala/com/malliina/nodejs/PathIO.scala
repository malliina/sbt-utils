package com.malliina.nodejs

import sbt.{File, Logger}

import java.nio.file.Path
import scala.sys.process.Process

object PathIO {
  val isWindows = sys.props("os.name").toLowerCase().contains("win")
  val cmdPrefix = if (isWindows) Seq("cmd", "/c") else Nil

  def runCommand(command: String, cwd: Path, log: Logger) =
    runProcessSync(command.split(" "), cwd, log)

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

  def canonical(cmd: Seq[String]): Seq[String] = cmdPrefix ++ cmd
}
