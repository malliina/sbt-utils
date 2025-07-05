package com.malliina.server

import org.http4s.Uri

trait AssetsSource:
  def at(file: String): Uri

object DirectAssets extends AssetsSource:
  override def at(file: String): Uri = Uri.unsafeFromString(s"/assets/out/$file")
