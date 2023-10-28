package com.malliina.sbt.filetree

import java.nio.charset.StandardCharsets

import com.malliina.sbt.filetree.FileTreeKeys.fileTreeSources
import com.malliina.sbt.filetree.ScalaIdentifiers.legalName
import sbt.Keys.{sourceGenerators, sourceManaged}
import sbt._
import sbt.plugins.JvmPlugin

object FileTreePlugin extends AutoPlugin {
  override def requires: Plugins = JvmPlugin

  override def projectSettings: Seq[Setting[?]] = Seq(
    fileTreeSources := Nil,
    Compile / sourceGenerators += Def.task {
      val dest = (Compile / sourceManaged).value
      fileTreeSources.value.flatMap(mapping => makeSources(mapping, dest))
    }.taskValue
  )

  val autoImport = FileTreeKeys

  def makeSources(mapping: DirMap, destBase: File): Seq[File] = {
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
    IO.write(destFile, content, StandardCharsets.UTF_8)
    Seq(destFile)
  }

  def members(dir: File, parent: String): String = {
    val paths = IO.listFiles(dir)
    val dirs = paths.filter(_.isDirectory).map(dir => makeDir(dir, parent)).mkString("")
    val defs = makeDefs(paths.filter(_.isFile))
    Seq(dirs, defs).mkString(IO.Newline)
  }

  def makeDir(dir: File, parent: String): String = {
    val newParent = s"$parent${dir.base}/"
    val inner = members(dir, newParent)
    val objName = legalName(dir.base)
    s"""
       |object $objName extends Dir("$newParent") {
       |$inner
       |}
    """.stripMargin.trim + IO.Newline
  }

  def makeDefs(files: Seq[File]) =
    files.map(makeFile).mkString(IO.Newline)

  def makeFile(file: File) = {
    val defName = legalName(file.name)
    s"""def $defName: T = map(prefix + "${file.getName}")"""
  }

  def destDir(base: File, packageName: String): File =
    packageName.split('.').foldLeft(base)((acc, part) => acc / part)
}
