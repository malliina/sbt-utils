package com.malliina.filetree

import com.malliina.filetree.FileTreeKeys.fileTreeSources
import com.malliina.filetree.ScalaIdentifiers.legalName
import sbt.*
import sbt.Keys.{sourceGenerators, sourceManaged}
import sbt.plugins.JvmPlugin

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import scala.jdk.CollectionConverters.iterableAsScalaIterableConverter

object FileTreePlugin extends AutoPlugin {
  override def requires: Plugins = JvmPlugin

  override def projectSettings: Seq[Setting[?]] = Seq(
    fileTreeSources := Nil,
    Compile / sourceGenerators += Def.task {
      val dest = (Compile / sourceManaged).value.toPath
      fileTreeSources.value.flatMap(mapping => makeSources(mapping, dest).map(_.toFile))
    }.taskValue
  )

  val autoImport = FileTreeKeys

  def makeSources(mapping: DirMap, destBase: Path): Seq[Path] = {
    val packageName = mapping.packageName
    val className = mapping.className
    val mapFunc = mapping.mapFunc
    val inner = members(mapping.source, "")
    val content =
      s"""
         |package $packageName
         |
         |class Dir(protected val prefix: String)
         |
         |object $className extends $className($mapFunc)
         |
         |class $className[T](map: String => T) extends Dir("") {
         |$inner
         |}
      """.stripMargin.trim + IO.Newline
    val destFile = destDir(destBase, packageName) / s"$className.scala"
    IO.write(destFile.toFile, content, StandardCharsets.UTF_8)
    Seq(destFile)
  }

  def members(dir: Path, parent: String): String = {
    val paths = Files.list(dir).toList.asScala.toList
    val dirs = paths.filter(Files.isDirectory(_)).map(dir => makeDir(dir, parent)).mkString("")
    val defs = makeDefs(paths.filter(Files.isRegularFile(_)))
    Seq(dirs, defs).mkString(IO.Newline)
  }

  def makeDir(dir: Path, parent: String): String = {
    val base = dir.toFile.base
    val newParent = s"$parent$base/"
    val inner = members(dir, newParent)
    val objName = legalName(base)
    s"""
       |object $objName extends Dir("$newParent") {
       |$inner
       |}
    """.stripMargin.trim + IO.Newline
  }

  def makeDefs(files: Seq[Path]) =
    files.map(makeFile).mkString(IO.Newline)

  def makeFile(file: Path) = {
    val defName = legalName(file.toFile.name)
    s"""def $defName: T = map(prefix + "${file.toFile.getName}")"""
  }

  def destDir(base: Path, packageName: String): Path =
    packageName.split('.').foldLeft(base)((acc, part) => acc / part)
}
