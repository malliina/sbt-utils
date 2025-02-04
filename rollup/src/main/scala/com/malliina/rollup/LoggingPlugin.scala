package com.malliina.rollup

import com.malliina.rollup.LoggingPlugin.autoImport.{initLogging, initLoggingOnStartup}
import org.apache.logging.log4j
import org.apache.logging.log4j.core.config.Configurator
import sbt.Keys.{logLevel, onLoad, streams}
import sbt.{AutoPlugin, Def, Global, Level, settingKey, taskKey}

object LoggingPlugin extends AutoPlugin {
  object autoImport {
    val initLoggingOnStartup = settingKey[Boolean]("True to init logging on startup.")
    val initLogging =
      taskKey[Unit]("Init logging so that logs of SLF4J-using libraries are printed.")
  }

  override def projectSettings: Seq[Def.Setting[?]] = Seq(
    initLoggingOnStartup := true,
    logLevel := Level.Info,
    initLogging := {
      val levelToLevel = logLevel.value match {
        case Level.Debug => log4j.Level.DEBUG
        case Level.Info  => log4j.Level.INFO
        case Level.Warn  => log4j.Level.WARN
        case Level.Error => log4j.Level.ERROR
      }
      streams.value.log.debug(s"Set log level to ${levelToLevel.name()}.")
      Configurator.setRootLevel(levelToLevel)
    },
    Global / onLoad := (Global / onLoad).value.andThen { state =>
      if (initLoggingOnStartup.value) "initLogging" :: state
      else state
    }
  )
}
