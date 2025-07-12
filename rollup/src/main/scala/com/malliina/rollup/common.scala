package com.malliina.rollup

import com.malliina.storage.{StorageLong, StorageSize}
import io.circe.generic.semiauto.deriveEncoder
import io.circe.syntax.EncoderOps
import io.circe.{Encoder, Json}
import sbt.{settingKey, taskKey}

import java.nio.file.{Files, Path}
import scala.sys.process.Process
import scala.util.Try

object CommonKeys {
  val assetsPrefix = settingKey[String]("I don't know what this is")
  val assetsRoot = settingKey[Path]("Assets root directory")
  val build = taskKey[Unit]("Builds app") // Consider replacing with compile
  val deploy = taskKey[Unit]("Deploys the site")
  val isProd = settingKey[Boolean]("true if in prod mode, false otherwise")
  val start = taskKey[Unit]("Starts the project")
}

object Git {
  def gitHash: String =
    sys.env
      .get("GITHUB_SHA")
      .orElse(Try(Process("git rev-parse HEAD").lineStream.head).toOption)
      .getOrElse("unknown")
}

case class HashedFile(path: String, hashedPath: String, originalFile: Path, hashedFile: Path) {
  def size = Files.size(originalFile).bytes
}

object HashedFile {
  def from(original: Path, hashed: Path, root: Path) = HashedFile(
    root.relativize(original).toString.replace('\\', '/'),
    root.relativize(hashed).toString.replace('\\', '/'),
    original,
    hashed
  )
}

case class UrlOption(filter: String, url: String, maxSize: Option[StorageSize])

object UrlOption {
  implicit val sizeEncoder: Encoder[StorageSize] =
    Encoder.encodeLong.contramap[StorageSize](_.toKilos)
  private val basic: Encoder[UrlOption] = deriveEncoder[UrlOption]
  private val copy = Json.obj(
    "fallback" -> "copy".asJson,
    "assetsPath" -> "assets".asJson,
    "useHash" -> true.asJson,
    "hashOptions" -> Json.obj("append" -> true.asJson)
  )
  implicit val json: Encoder[UrlOption] = (uo: UrlOption) =>
    uo.maxSize.fold(basic(uo))(_ => basic(uo).deepMerge(copy))
  val defaults =
    Seq(exts(Seq("woff", "woff2", "png", "svg"), 64.kilos), anyParent(Option(16.kilos)), anySibling)
  def anyParent(maxSize: Option[StorageSize]): UrlOption =
    inline("../**/*", maxSize)
  def anySibling = inline("**/*", Option(1.kilos))
  def exts(es: Seq[String], maxSize: StorageSize): UrlOption = {
    val extsStr = es.mkString("|")
    val minimatch = s"../**/*.+($extsStr)"
    inline(minimatch, Option(maxSize))
  }
  def inline(minimatch: String, maxSize: Option[StorageSize]) =
    UrlOption(minimatch, "inline", maxSize)
}
