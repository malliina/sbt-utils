package com.malliina.live

import com.malliina.live.LiveReloadPlugin.autoImport.{refreshBrowsers, reloader}
import com.malliina.live.LiveRevolverPlugin.autoImport.startApp
import sbt.*
import spray.revolver.RevolverKeys.{reStart, reStop}
import spray.revolver.{AppProcess, RevolverPlugin}

object LiveRevolverPlugin extends AutoPlugin:
  override def requires = LiveReloadPlugin && RevolverPlugin

  object autoImport:
    val startApp = taskKey[Unit]("Starts app")

  override def projectSettings: Seq[Def.Setting[?]] = Seq(
    reStart := reStart.dependsOn(Def.task(reloader.value.start())).evaluated,
    reStop := reStop.dependsOn(Def.task(reloader.value.close())).value,
    startApp := Def.uncached(reStart.toTask(" ").value),
    startApp := refreshBrowsers.dependsOn(startApp).value
  )
