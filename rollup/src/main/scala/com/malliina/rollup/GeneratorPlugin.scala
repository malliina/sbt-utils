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
import sbtbuildinfo.Entry.Constant
import sbtbuildinfo.{BuildInfoKey, BuildInfoPlugin, PluginCompat}

object GeneratorPlugin extends AutoPlugin:
  override def requires: Plugins =
    BuildInfoPlugin && LiveReloadPlugin && HashPlugin && FileTreePlugin

  object autoImport:
    val scalajsProject = settingKey[ProjectRef]("Scala.js project")
  import autoImport.*

  override def projectSettings: Seq[Setting[?]] = Seq(
    isProd := scalaJSStage.value == FullOptStage,
    assetsRoot := Def
      .settingDyn:
        scalajsProject.value / assetsRoot
      .value,
    hashRoot := assetsRoot.value,
    liveReloadRoot := assetsRoot.value,
    buildInfoKeys ++= Seq[PluginCompat.Entry[?]](
      Constant("siteDir" -> assetsRoot.value.toFile),
      Constant("isProd" -> isProd.value),
      Constant("gitHash" -> Git.gitHash)
    ),
    build := Def.uncached:
      Def
        .taskDyn:
          (Compile / run)
            .toTask(" ")
            .dependsOn(Def.task(if isProd.value then () else reloader.value.start()))
        .value
    ,
    build := refreshBrowsers.dependsOn(build).value,
    watchSources := Def.uncached:
      watchSources.value ++ Def.taskDyn(scalajsProject.value / watchSources).value
    ,
    Compile / sourceGenerators += hash.map(_.map(_.toFile)),
    Compile / compile := Def.uncached:
      (Compile / compile)
        .dependsOn(hash)
        .dependsOn(Def.taskDyn(scalajsProject.value / build))
        .value
    ,
    fileTreeSources += DirMap(assetsRoot.value, s"${hashPackage.value}.FileAssets")
  )
