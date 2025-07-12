package com.malliina.rollup

import com.malliina.build.FileIO
import io.circe.parser.parse
import sbt.*

import java.nio.charset.StandardCharsets
import java.nio.file.Path
import scala.sys.process.Process

object IO {
  val utf8 = StandardCharsets.UTF_8
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

  def render(path: Path): String = {
    val str = path.toString
    if (isWindows) str.replaceAll("\\\\", "\\\\\\\\")
    else str
  }

  def writePackageJsonIfChanged(resDir: Path, destDir: Path, inbuiltResource: String): Boolean = {
    val userPackageJson = resDir / "package.json"
    val inbuilt = json(res(inbuiltResource))
    val packageJson =
      if (userPackageJson.toFile.exists())
        inbuilt.deepMerge(jsonFile(userPackageJson.toFile))
      else
        inbuilt
    FileIO.writeIfChanged(packageJson.spaces2SortKeys, destDir.resolve("package.json"))
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
