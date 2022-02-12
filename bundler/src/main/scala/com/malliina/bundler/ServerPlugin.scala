package com.malliina.bundler

import com.malliina.bundler.ClientPlugin.autoImport.writeAssets
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport.{fastOptJS, fullOptJS, scalaJSStage}
import org.scalajs.sbtplugin.Stage
import sbt.Keys._
import sbt.nio.Keys._
import sbt._
import spray.revolver.RevolverPlugin.autoImport.reStart
import spray.revolver.{GlobalState, RevolverPlugin}

object ServerPlugin extends AutoPlugin {
  override def requires = RevolverPlugin
  object autoImport {
    val start = Keys.start
    val clientProject = settingKey[Project]("Scala.js project")
  }
  import autoImport._

  override def projectSettings: Seq[Def.Setting[_]] = Seq(
    start / fileInputs ++=
      (Compile / sourceDirectories).value.map(_.toGlob / ** / "*.scala") ++
        (Compile / resourceDirectories).value.map(_.toGlob / **),
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
