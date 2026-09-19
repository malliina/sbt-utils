package com.malliina.live

import com.jamesward.sbtreload.ReloadPlugin.autoImport.runReload
import com.malliina.live.LiveReloadPlugin.autoImport.{refreshBrowsers, reloader}
import com.malliina.live.LiveRevolverPlugin.autoImport.{dev, startApp}
import sbt.*
import spray.revolver.RevolverKeys.{reStart, reStop}
import spray.revolver.{AppProcess, RevolverPlugin}

object LiveRevolverPlugin extends AutoPlugin:
  override def requires = LiveReloadPlugin && RevolverPlugin

  object autoImport:
    val startApp = taskKey[Unit]("Starts app")
    val dev = taskKey[Unit]("Starts app with auto-reload")

  override def projectSettings: Seq[Def.Setting[?]] = Seq(
    reStart := reStart.dependsOn(Def.task(reloader.value.start())).evaluated,
    reStop := reStop.dependsOn(Def.task(reloader.value.close())).value,
    startApp := Def.uncached(reStart.toTask(" ").value),
    startApp := refreshBrowsers.dependsOn(startApp).value,
    dev := refreshBrowsers.dependsOn(Compile / runReload, Def.task(reloader.value.start())).value
  )
