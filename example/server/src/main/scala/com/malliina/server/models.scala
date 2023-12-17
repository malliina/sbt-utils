package com.malliina.server

import io.circe.Codec

case class IndexResponse(message: String) derives Codec.AsObject
object IndexResponse:
  val default = IndexResponse("hi")

case class ErrorMessage(message: String) derives Codec.AsObject

case class Errors(errors: Seq[ErrorMessage]) derives Codec.AsObject
object Errors:
  def single(message: String) = Errors(Seq(ErrorMessage(message)))
