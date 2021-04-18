package com.malliina.bundler

import ClientPlugin.autoImport.{allAssets, assetsDir, writeAssets, sjsTask}
import org.scalajs.linker.interface.Report
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport.{
  fastLinkJS,
  fastOptJS,
  fullLinkJS,
  fullOptJS,
  scalaJSStage
}
import org.scalajs.sbtplugin.Stage
import sbt.Keys._
import sbt._
import scalajsbundler.sbtplugin.ScalaJSBundlerPlugin.autoImport.webpack
import spray.revolver.{AppProcess, RevolverPlugin}
import spray.revolver.RevolverPlugin.autoImport.reStart

object ServerPlugin extends AutoPlugin {
  override def requires = RevolverPlugin
  object autoImport {
    val clientProject = settingKey[Project]("Scala.js project")
  }
  import autoImport._

  val clientDyn = Def.settingDyn(clientProject)

  override def projectSettings: Seq[Def.Setting[_]] = Seq(
    Compile / resources ++= Def.taskDyn {
      val sjsStage = (clientProject / scalaJSStage).value match {
        case Stage.FastOpt => fastOptJS
        case Stage.FullOpt => fullOptJS
      }
      clientProject.value / Compile / sjsStage / allAssets
    }.value,
    Compile / resourceDirectories += Def
      .settingDyn(clientDyn.value / Compile / assetsDir)
      .value
      .toFile,
    reStart := Def
      .inputTaskDyn[AppProcess] {
        reStart
          .toTask(" ")
          .dependsOn(clientDyn.value / Compile / fastOptJS / webpack)
      }
      .evaluated,
    watchSources ++= (clientProject / watchSources).value,
    Compile / sourceGenerators := (Compile / sourceGenerators).value :+ Def
      .taskDyn[Seq[File]] {
        val sjsStage = (clientProject / scalaJSStage).value match {
          case Stage.FastOpt => fastOptJS
          case Stage.FullOpt => fullOptJS
        }
        val client = clientProject.value
        clientProject.value / Compile / sjsStage / writeAssets
      }
      .taskValue
  )
}
