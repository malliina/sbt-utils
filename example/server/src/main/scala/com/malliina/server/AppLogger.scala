package com.malliina.server

import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.{Level, Logger, LoggerContext}
import ch.qos.logback.core.{Appender, ConsoleAppender}
import org.slf4j.{LoggerFactory, Logger as SLF4JLogger}

object AppLogger:
  def apply(cls: Class[?]): SLF4JLogger =
    val name = cls.getName.reverse.dropWhile(_ == '$').reverse
    LoggerFactory.getLogger(name)

  def init(
    pattern: String = """%d{HH:mm:ss.SSS} %-5level %logger{72} %msg%n""",
    rootLevel: Level = Level.INFO,
    levelsByLogger: Map[String, Level] = Map.empty
  ): LoggerContext =
    val lc = LoggerFactory.getILoggerFactory.asInstanceOf[LoggerContext]
    lc.reset()
    val ple = PatternLayoutEncoder()
    ple.setPattern(pattern)
    ple.setContext(lc)
    ple.start()
    val console = new ConsoleAppender[ILoggingEvent]()
    console.setEncoder(ple)
    installAppender(console)
    val root = lc.getLogger(SLF4JLogger.ROOT_LOGGER_NAME)
    root.setLevel(rootLevel)
    levelsByLogger.foreach { (loggerName, level) =>
      lc.getLogger(loggerName).setLevel(level)
    }
    lc

  def installAppender(
    appender: Appender[ILoggingEvent],
    loggerName: String = SLF4JLogger.ROOT_LOGGER_NAME
  ): Unit =
    if appender.getContext == null then
      appender.setContext(LoggerFactory.getILoggerFactory.asInstanceOf[LoggerContext])
    if !appender.isStarted then appender.start()
    val logger = LoggerFactory.getLogger(loggerName).asInstanceOf[Logger]
    logger.addAppender(appender)
