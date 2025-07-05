package com.malliina.server

import cats.data.NonEmptyList
import cats.effect.Async
import cats.implicits.*
import com.malliina.server.StaticService.log
import org.http4s.CacheDirective.*
import org.http4s.headers.`Cache-Control`
import org.http4s.{Header, HttpRoutes, Request, StaticFile}
import org.typelevel.ci.CIStringSyntax

import scala.concurrent.duration.DurationInt

object StaticService:
  private val log = AppLogger(getClass)

class StaticService[F[_]: Async] extends WebSyntax[F]:
  private val fontExtensions = List(".woff", ".woff2", ".eot", ".ttf")
  private val supportedStaticExtensions =
    List(".html", ".js", ".map", ".css", ".png", ".ico", ".svg") ++ fontExtensions

  private val allowAllOrigins = Header.Raw(ci"Access-Control-Allow-Origin", "*")
  private val assetsDir = fs2.io.file.Path(BuildInfo.assetsDir.getAbsolutePath)
  val routes: HttpRoutes[F] = HttpRoutes.of[F]:
    case req @ GET -> rest if supportedStaticExtensions.exists(rest.toString.endsWith) =>
      val file = rest.segments.mkString("/")
      val isCacheable = file.count(_ == '.') == 2
      val cacheHeaders =
        if isCacheable then NonEmptyList.of(`max-age`(365.days), `public`)
        else WebSyntax.noCacheDirectives
      val search =
        if BuildInfo.isProd then
          val resourcePath = s"${BuildInfo.publicFolder}/$file"
          log.debug(s"Searching for resource '$resourcePath'...")
          StaticFile.fromResource(resourcePath, Option(req))
        else
          val assetPath: fs2.io.file.Path = assetsDir.resolve(file)
          log.debug(
            s"Searching for file '${assetPath.toNioPath.toAbsolutePath}'..."
          )
          StaticFile
            .fromPath(assetPath, Option(req))
      search
        .map(_.putHeaders(`Cache-Control`(cacheHeaders), allowAllOrigins))
        .fold(onNotFound(req))(_.pure[F])
        .flatten

  private def onNotFound(req: Request[F]) =
    log.info(s"Not found '${req.uri}'.")
    notFound(req)
