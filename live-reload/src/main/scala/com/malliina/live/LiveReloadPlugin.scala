package com.malliina.live

import com.comcast.ip4s.{Host, Port, host, port}
import sbt.*
import sbt.Keys.*

import java.nio.charset.StandardCharsets
import java.nio.file.Path

object LiveReloadPlugin extends AutoPlugin:
  object autoImport:
    val reloader = settingKey[Reloadable]("Interface to browsers")
    val liveReloadRoot = settingKey[Path]("Path to live reload root for serving static files")
    val liveReloadHost = settingKey[Host]("Host for live reload, defaults to localhost")
    val liveReloadPort = settingKey[Port]("HTTP port for live reload, defaults to 10101")
    val refreshBrowsers = taskKey[Unit]("Refreshes browsers")
  import autoImport.*

  override def projectSettings: Seq[Setting[?]] = Seq(
    liveReloadRoot := io.Path.userHome.toPath.resolve(".live-reload"),
    liveReloadHost := host"localhost",
    liveReloadPort := port"10104",
    reloader := new OnOffReloadable(
      StaticServer.start(
        liveReloadRoot.value,
        liveReloadHost.value,
        liveReloadPort.value,
        sLog.value
      ),
      NoopReloadable
    ),
    Global / onUnload := {
      val first = (Global / onUnload).value
      sLog.value.info("Shutting down...")
      reloader.value.close()
      first
    },
    refreshBrowsers := Def.uncached:
      sLog.value.info("Refreshing browsers...")
      reloader.value.reload()
    ,
//    extraAppenders := {
//      class BrowserConsoleAppender(key: ScopedKey[?]) extends Appender {
//        override def close(): Unit = ()
//      }
//      val currentFunction = extraAppenders.value
//      (key: ScopedKey[?]) => {
//        (new BrowserConsoleAppender(key)) +: currentFunction(key)
//      }
//    },
    Compile / sourceGenerators += Def
      .task:
        val dest = (Compile / sourceManaged).value
        makeSources(dest, reloader.value)
      .taskValue
  )

  private def makeSources(destBase: File, server: Reloadable): Seq[File] =
    val packageName = "com.malliina.live"
    val host = s"http://localhost:${server.port}"
    val content =
      s"""
         |package $packageName
         |
         |object LiveReload {
         |  val host = "$host"
         |  val script = "${server.scriptUrl}"
         |  val socket = "${server.wsUrl}"
         |  val isEnabled = ${server.isEnabled}
         |}
      """.stripMargin.trim + IO.Newline
    val destFile = destDir(destBase, packageName) / "LiveReload.scala"
    IO.write(destFile, content, StandardCharsets.UTF_8)
    Seq(destFile)

  private def destDir(base: File, packageName: String): File =
    packageName.split('.').foldLeft(base)((acc, part) => acc / part)
