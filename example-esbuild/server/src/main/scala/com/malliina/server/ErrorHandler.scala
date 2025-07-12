package com.malliina.server

import cats.effect.Async
import com.malliina.server.ErrorHandler.log
import io.circe.syntax.EncoderOps
import org.http4s.*
import org.http4s.circe.CirceEntityEncoder.circeEntityEncoder
import org.http4s.dsl.Http4sDsl
import java.io.IOException
import scala.util.control.NonFatal

object ErrorHandler:
  private val log = AppLogger(getClass)

class ErrorHandler[F[_]: Async] extends Http4sDsl[F]:
  def partial: PartialFunction[Throwable, F[Response[F]]] =
    case ioe: IOException if ioe.getMessage == "Connection reset" =>
      InternalServerError(Errors.single(s"Server error.").asJson, WebSyntax.noCache)
    case NonFatal(t) =>
      log.error(s"Server error.", t)
      InternalServerError(Errors.single(s"Server error.").asJson, WebSyntax.noCache)
