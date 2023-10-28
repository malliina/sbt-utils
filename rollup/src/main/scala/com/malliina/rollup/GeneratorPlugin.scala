package com.malliina.rollup

import com.malliina.filetree.FileTreeKeys.fileTreeSources
import com.malliina.filetree.{DirMap, FileTreePlugin}
import com.malliina.live.LiveReloadPlugin
import com.malliina.live.LiveReloadPlugin.autoImport.{liveReloadRoot, refreshBrowsers, reloader}
import com.malliina.rollup.CommonKeys.{assetsRoot, build, isProd}
import com.malliina.rollup.HashPlugin.autoImport.{hash, hashPackage, hashRoot}
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport.{FullOptStage, scalaJSStage}
import sbt.*
import sbt.Keys.{compile, run, sourceGenerators, watchSources}
import sbtbuildinfo.BuildInfoKeys.buildInfoKeys
import sbtbuildinfo.{BuildInfoKey, BuildInfoPlugin}

object GeneratorPlugin extends AutoPlugin {
  override def requires = BuildInfoPlugin && LiveReloadPlugin && HashPlugin && FileTreePlugin

  object autoImport {
    val scalajsProject = settingKey[Project]("Scala.js project")
  }
  import autoImport.*

  override def projectSettings: Seq[Setting[?]] = Seq(
    isProd := scalaJSStage.value == FullOptStage,
    assetsRoot := Def.settingDyn { scalajsProject.value / assetsRoot }.value,
    hashRoot := assetsRoot.value,
    liveReloadRoot := assetsRoot.value,
    buildInfoKeys ++= Seq[BuildInfoKey](
      "siteDir" -> assetsRoot.value.toFile,
      "isProd" -> isProd.value,
      "gitHash" -> Git.gitHash
    ),
    refreshBrowsers := refreshBrowsers.triggeredBy(build).value,
    build := Def.taskDyn {
      (Compile / run)
        .toTask(" ")
        .dependsOn(Def.task(if (isProd.value) () else reloader.value.start()))
    }.value,
    watchSources := watchSources.value ++ Def.taskDyn(scalajsProject.value / watchSources).value,
    Compile / sourceGenerators += hash.map(_.map(_.toFile)),
    Compile / compile := (Compile / compile)
      .dependsOn(hash)
      .dependsOn(Def.taskDyn(scalajsProject.value / build))
      .value,
    fileTreeSources += DirMap(assetsRoot.value, s"${hashPackage.value}.FileAssets")
  )
}
