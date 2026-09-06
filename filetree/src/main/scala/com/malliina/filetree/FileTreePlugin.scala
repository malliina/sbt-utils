package com.malliina.filetree

import com.malliina.filetree.FileTreeKeys.{fileTreeSources, scalafmtConf, writeFileTree}
import com.malliina.filetree.ScalaIdentifiers.legalName
import org.scalafmt.dynamic.coursier.CoursierDependencyDownloaderFactory
import sbt.*
import sbt.Keys.{sourceManaged, streams}
import sbt.plugins.JvmPlugin

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path, Paths}
import scala.jdk.CollectionConverters.CollectionHasAsScala

import org.scalafmt.interfaces.Scalafmt

object FileTreePlugin extends AutoPlugin:
  private val scalafmt = Scalafmt
    .create(this.getClass.getClassLoader)
    .withRepositoryPackageDownloader(CoursierDependencyDownloaderFactory)

  override def requires: Plugins = JvmPlugin

  override def projectSettings: Seq[Setting[?]] = Seq(
    fileTreeSources := Nil,
    scalafmtConf := Option(Paths.get(".scalafmt.conf")),
    writeFileTree := Def.uncached:
      val dest = (Compile / sourceManaged).value.toPath
      fileTreeSources.value.flatMap: mapping =>
        streams.value.log.info(s"Writing file tree of ${mapping.source} to $dest...")
        makeSources(mapping, dest, scalafmtConf.value).map(_.toFile)
//    Compile / sourceGenerators += writeFileTree.taskValue
  )

  val autoImport = FileTreeKeys

  private def makeSources(
    mapping: DirMap,
    destBase: Path,
    scalafmtConfFile: Option[Path]
  ): Seq[Path] =
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
    val formatted =
      scalafmtConfFile
        .filter(p => Files.exists(p))
        .map(conf => scalafmt.format(conf, destFile, content))
        .getOrElse(content)
    IO.write(destFile.toFile, formatted, StandardCharsets.UTF_8)
    Seq(destFile)

  private def members(dir: Path, parent: String): String =
    val paths = Files.list(dir).toList.asScala.toList
    val dirs = paths.filter(Files.isDirectory(_)).map(dir => makeDir(dir, parent)).mkString("")
    val defs = makeDefs(paths.filter(Files.isRegularFile(_)))
    Seq(dirs, defs).mkString(IO.Newline)

  private def makeDir(dir: Path, parent: String): String =
    val base = dir.toFile.base
    val newParent = s"$parent$base/"
    val inner = members(dir, newParent)
    val objName = legalName(base)
    s"""
       |object $objName extends Dir("$newParent") {
       |$inner
       |}
    """.stripMargin.trim + IO.Newline

  private def makeDefs(files: Seq[Path]) =
    files.map(makeFile).mkString(IO.Newline)

  private def makeFile(file: Path) =
    val defName = legalName(file.toFile.name)
    s"""def $defName: T = map(prefix + "${file.toFile.getName}")"""

  def destDir(base: Path, packageName: String): Path =
    packageName.split('.').foldLeft(base)((acc, part) => acc / part)
