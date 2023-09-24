package com.malliina.bundler

import com.malliina.bundler.ClientPlugin.autoImport.writeAssets
import com.malliina.live.LiveReloadPlugin.autoImport.refreshBrowsers
import com.malliina.live.LiveRevolverPlugin
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport.{fastLinkJS, fastOptJS, fullLinkJS, fullOptJS, scalaJSStage}
import org.scalajs.sbtplugin.Stage
import sbt.Keys.*
import sbt.*
import spray.revolver.RevolverPlugin.autoImport.reStart
import spray.revolver.GlobalState

object ServerPlugin extends AutoPlugin {
  override def requires: Plugins = LiveRevolverPlugin && FileInputPlugin
  object autoImport {
    val start = BundlerKeys.start
    val clientProject = settingKey[Project]("Scala.js project")
  }
  import autoImport._

  override def projectSettings: Seq[Def.Setting[?]] = Seq(
    start := Def.taskIf {
      val log = streams.value.log
      val changes = start.inputFileChanges
      // Restarts if a) not running, or b) input files have changed
      val isRunning = GlobalState.get().getProcess(thisProjectRef.value).isDefined
      val word = if (isRunning) "" else "not "
      val fileWord = if (changes.hasChanges) "" else "not"
      log.debug(s"${name.value} ${word}running. Files ${fileWord}changed.")
      if (changes.hasChanges || !isRunning) {
        reStart.toTask(" ").value
      } else {
        Def.task(streams.value.log.info(s"No changes to ${name.value}, no restart.")).value
      }
    }.value,
    start := Def.taskIf {
      if (start.inputFileChanges.hasChanges) {
        refreshBrowsers.value
      } else {
        Def.task(streams.value.log.info("No backend changes."))
      }
    }.dependsOn(start).value,
    Compile / sourceGenerators := (Compile / sourceGenerators).value :+ Def
      .taskDyn[Seq[File]] {
        val sjsStage = (clientProject / scalaJSStage).value match {
          case Stage.FastOpt => fastOptJS
          case Stage.FullOpt => fullOptJS
        }
        clientProject.value / Compile / sjsStage / writeAssets
      }
      .taskValue
  )
}
