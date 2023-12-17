package com.malliina.server

import cats.effect.Async
import com.malliina.server.ErrorHandler.{log, noCache}
import io.circe.syntax.EncoderOps
import org.http4s.*
import org.http4s.CacheDirective.*
import org.http4s.circe.CirceEntityEncoder.circeEntityEncoder
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.{Accept, Location, `Cache-Control`, `WWW-Authenticate`}

import scala.util.control.NonFatal

object ErrorHandler:
  private val log = AppLogger(getClass)
  val noCache = `Cache-Control`(`no-cache`(), `no-store`, `must-revalidate`)

class ErrorHandler[F[_]: Async] extends Http4sDsl[F]:
  def partial: PartialFunction[Throwable, F[Response[F]]] =
    case NonFatal(t) =>
      log.error(s"Server error.", t)
      InternalServerError(Errors.single(s"Server error.").asJson, noCache)
