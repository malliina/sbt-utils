package com.malliina.rollup

import com.malliina.filetree.FileTreeKeys.fileTreeSources
import com.malliina.filetree.{DirMap, FileTreePlugin}
import com.malliina.live.LiveReloadPlugin.autoImport.refreshBrowsers
import com.malliina.live.LiveRevolverPlugin
import com.malliina.rollup.CommonKeys.{assetsRoot, build, isProd}
import com.malliina.rollup.HashPlugin.autoImport.{copyFolders, hash, hashPackage, hashRoot, useHash}
import com.malliina.rollup.RollupPlugin.autoImport.assetsPrefix
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport.{FullOptStage, scalaJSStage}
import sbt.*
import sbt.Keys.*
import sbtbuildinfo.BuildInfoKeys.buildInfoKeys
import sbtbuildinfo.BuildInfoPlugin
import sbtbuildinfo.BuildInfoPlugin.autoImport.BuildInfoKey
import spray.revolver.GlobalState
import spray.revolver.RevolverPlugin.autoImport.reStart

object ServerPlugin extends AutoPlugin {
  override def requires: Plugins =
    LiveRevolverPlugin && FileInputPlugin && HashPlugin && BuildInfoPlugin && FileTreePlugin
  object autoImport {
    val clientProject = settingKey[Project]("Scala.js project")
    val start = CommonKeys.start
  }
  import autoImport.*

  override def projectSettings: Seq[Def.Setting[?]] = Seq(
    isProd := scalaJSStage.value == FullOptStage,
    useHash := isProd.value,
    start := Def.taskIf {
      val log = streams.value.log
      val changes = start.inputFileChanges
      // Restarts if a) not running, or b) input files have changed
      val isRunning = GlobalState.get().getProcess(thisProjectRef.value).isDefined
      val word = if (isRunning) "" else "not "
      val fileWord = if (changes.hasChanges) "" else "not "
      log.debug(s"${name.value} ${word}running. Files ${fileWord}changed.")
      if (changes.hasChanges || !isRunning) {
        reStart.toTask(" ").dependsOn(hash).value
      } else {
        Def.task(streams.value.log.info(s"No changes to ${name.value}, no restart.")).value
      }
    }.value, // sbt warns about pure statement, but without .value this does not work at all
    start := start.dependsOn(Def.taskDyn(clientProject.value / build)).value,
    refreshBrowsers := refreshBrowsers
      .triggeredBy(Def.taskDyn(clientProject.value / build), start)
      .value,
    watchSources := watchSources.value ++ Def.taskDyn(clientProject.value / watchSources).value,
    hashRoot := Def.settingDyn(clientProject.value / assetsRoot).value,
    hash := hash
      .dependsOn(Def.taskDyn(clientProject.value / build))
      .value,
    Compile / compile := (Compile / compile).dependsOn(hash).value,
    Compile / sourceGenerators += hash.map(_.map(_.toFile)),
    copyFolders += ((Compile / resourceDirectory).value / "public").toPath,
    buildInfoKeys ++= Seq[BuildInfoKey](
      "gitHash" -> Git.gitHash,
      "assetsDir" -> Def.settingDyn(clientProject.value / assetsRoot).value.toFile,
      "publicDir" -> (Compile / resourceDirectory).value.toPath.resolve("public"),
      "publicFolder" -> Def.settingDyn(clientProject.value / assetsPrefix).value,
      "mode" -> (if (isProd.value) "prod" else "dev"),
      "isProd" -> isProd.value
    ),
    Compile / unmanagedResourceDirectories ++= {
      if (isProd.value)
        List(Def.settingDyn(clientProject.value / assetsRoot).value.getParent.toFile)
      else
        Nil
    },
    fileTreeSources += DirMap(hashRoot.value, s"${hashPackage.value}.FileAssets")
  )
}
