package com.malliina.rollup

import com.malliina.filetree.FileTreeKeys.{fileTreeSources, writeFileTree}
import com.malliina.filetree.{DirMap, FileTreePlugin}
import com.malliina.live.LiveReloadPlugin.autoImport.refreshBrowsers
import com.malliina.live.LiveRevolverPlugin
import com.malliina.rollup.CommonKeys.{assetsPrefix, assetsRoot, build, isProd}
import com.malliina.rollup.HashPlugin.autoImport.{copyFolders, hash, hashPackage, hashRoot, useHash}
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport.{FullOptStage, scalaJSStage}
import sbt.*
import sbt.Keys.*
import sbtbuildinfo.BuildInfoKeys.buildInfoKeys
import sbtbuildinfo.Entry.Constant
import sbtbuildinfo.{BuildInfoPlugin, PluginCompat}
import spray.revolver.RevolverKeys.reStart

object ServerPlugin extends AutoPlugin:
  override def requires: Plugins =
    LiveRevolverPlugin && FileInputPlugin && HashPlugin && BuildInfoPlugin && FileTreePlugin
  object autoImport:
    val clientProject = settingKey[ProjectReference]("Scala.js project")
    val start = CommonKeys.start
    val hashAndWrite = taskKey[Seq[File]]("Hashes assets and writes a file tree.")
  import autoImport.*

  override def projectSettings: Seq[Def.Setting[?]] = Seq(
    isProd := scalaJSStage.value == FullOptStage,
    useHash := isProd.value,
    start := Def.uncached:
      reStart.toTask(" ").dependsOn(writeFileTree).value
    ,
    start := start.dependsOn(Def.taskDyn(clientProject.value / build)).value,
    start := refreshBrowsers.dependsOn(start).value,
    watchSources := Def.uncached:
      watchSources.value ++ Def.taskDyn(clientProject.value / watchSources).value
    ,
    hashRoot := Def.settingDyn(clientProject.value / assetsRoot).value,
    hash := Def.uncached:
      hash
        .dependsOn(Def.taskDyn(clientProject.value / build))
        .value
    ,
    writeFileTree := Def.uncached:
      writeFileTree.dependsOn(hash).value
    ,
    Compile / sourceGenerators ++= Seq(
      hash.taskValue,
      writeFileTree.taskValue
    ),
    copyFolders += ((Compile / resourceDirectory).value / "public").toPath,
    buildInfoKeys ++= Seq[PluginCompat.Entry[?]](
      Constant("gitHash" -> Git.gitHash),
      Constant("assetsDir" -> Def.settingDyn(clientProject.value / assetsRoot).value.toFile),
      Constant("publicDir" -> (Compile / resourceDirectory).value.toPath.resolve("public")),
      Constant("publicFolder" -> Def.settingDyn(clientProject.value / assetsPrefix).value),
      Constant("mode" -> (if isProd.value then "prod" else "dev")),
      Constant("isProd" -> isProd.value)
    ),
    Compile / unmanagedResourceDirectories ++= {
      if isProd.value then
        List(Def.settingDyn(clientProject.value / assetsRoot).value.getParent.toFile)
      else Nil
    },
    fileTreeSources += DirMap(hashRoot.value, s"${hashPackage.value}.FileAssets")
  )
