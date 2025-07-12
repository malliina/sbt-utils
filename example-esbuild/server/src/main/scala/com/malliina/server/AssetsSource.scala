package com.malliina.server

import com.malliina.assets.HashedAssets
import org.http4s.Uri

trait AssetsSource:
  def at(file: String): Uri

object DirectAssets extends AssetsSource:
  override def at(file: String): Uri = StaticService.assetsPrefix / file

object HashedAssetsSource extends AssetsSource:
  override def at(file: String): Uri =
    val optimal = HashedAssets.assets.getOrElse(file, file)
    StaticService.assetsPrefix / optimal
