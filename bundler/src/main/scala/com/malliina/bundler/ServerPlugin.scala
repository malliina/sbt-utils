package com.malliina.bundler

import com.malliina.bundler.ClientPlugin.autoImport.{allAssets, assetsDir, writeAssets}
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport.{fastOptJS, fullOptJS, scalaJSStage}
import org.scalajs.sbtplugin.Stage
import sbt.Keys._
import sbt._
import scalajsbundler.sbtplugin.ScalaJSBundlerPlugin.autoImport.webpack
import spray.revolver.RevolverPlugin.autoImport.reStart
import spray.revolver.{AppProcess, RevolverPlugin}

object ServerPlugin extends AutoPlugin {
  override def requires = RevolverPlugin
  object autoImport {
    val clientProject = settingKey[Project]("Scala.js project")
    val start = taskKey[AppProcess]("Like restart")
  }
  import autoImport._

  override def projectSettings: Seq[Def.Setting[_]] = Seq(
    start := Def.settingDyn {
      // Pattern adapted from https://github.com/scalacenter/scalajs-bundler/blob/0d8812f9d019ecf8aad151c54b98374b8aa87c9a/sbt-web-scalajs-bundler/src/main/scala/scalajsbundler/sbtplugin/WebScalaJSBundlerPlugin.scala#L65
      val p = Project.projectToRef(clientProject.value)
      Def.task {
        reStart.toTask(" ").dependsOn(p / Compile / fastOptJS / webpack).value
      }
    }.value,
    Compile / resources ++= Def.taskDyn {
      val sjsStage = (clientProject / scalaJSStage).value match {
        case Stage.FastOpt => fastOptJS
        case Stage.FullOpt => fullOptJS
      }
      clientProject.value / Compile / sjsStage / allAssets
    }.value,
    Compile / resourceDirectories += Def
      .settingDyn(clientProject.value / Compile / assetsDir)
      .value
      .toFile,
    watchSources ++= (clientProject / watchSources).value,
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
