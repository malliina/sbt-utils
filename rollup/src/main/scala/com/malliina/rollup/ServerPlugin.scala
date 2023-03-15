package com.malliina.rollup

import com.malliina.live.LiveReloadPlugin.autoImport.refreshBrowsers
import com.malliina.live.LiveRevolverPlugin
import com.malliina.rollup.CommonKeys.{assetsRoot, build}
import com.malliina.rollup.HashPlugin.autoImport.{copyFolders, hash, hashRoot, useHash}
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport.{FullOptStage, scalaJSStage}
import sbt._
import sbt.Keys._
import spray.revolver.GlobalState
import spray.revolver.RevolverPlugin.autoImport.reStart

object ServerPlugin extends AutoPlugin {
  override def requires: Plugins = LiveRevolverPlugin && FileInputPlugin && HashPlugin
  object autoImport {
    val clientProject = settingKey[Project]("Scala.js project")
    val start = CommonKeys.start
  }
  import autoImport._

  override def projectSettings: Seq[Def.Setting[?]] = Seq(
    useHash := scalaJSStage.value == FullOptStage,
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
    }.value,
    start := start.dependsOn(Def.taskDyn(clientProject.value / build)).value,
    refreshBrowsers := refreshBrowsers
      .triggeredBy(Def.taskDyn(clientProject.value / build), start)
      .value,
    watchSources := watchSources.value ++ Def.taskDyn(clientProject.value / watchSources).value,
    hashRoot := Def.settingDyn { clientProject.value / assetsRoot }.value,
    hash := hash
      .dependsOn(Def.taskDyn(clientProject.value / build))
      .value,
    Compile / compile := (Compile / compile).dependsOn(hash).value,
    Compile / sourceGenerators += hash.map(_.map(_.toFile)),
    copyFolders += ((Compile / resourceDirectory).value / "public").toPath
  )
}
