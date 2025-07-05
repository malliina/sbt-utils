package com.malliina.server

import cats.data.{Kleisli, NonEmptyList}
import cats.effect.kernel.Async
import cats.effect.{ExitCode, IO, IOApp}
import com.comcast.ip4s.{host, port}
import com.malliina.server.WebSyntax.noCache
import io.circe.syntax.EncoderOps
import org.http4s.CacheDirective.{`must-revalidate`, `no-cache`, `no-store`}
import org.http4s.circe.CirceInstances
import org.http4s.dsl.Http4sDsl
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.headers.`Cache-Control`
import org.http4s.server.Router
import org.http4s.server.middleware.{GZip, HSTS}
import org.http4s.{HttpRoutes, Request, Response, syntax}

import scala.concurrent.duration.DurationInt

object WebSyntax:
  val noCacheDirectives = NonEmptyList.of(`no-cache`(), `no-store`, `must-revalidate`)
  val noCache = `Cache-Control`(noCacheDirectives)

trait WebSyntax[F[_]: Async] extends syntax.AllSyntax with Http4sDsl[F] with CirceInstances:
  def notFound(req: Request[F]): F[Response[F]] =
    NotFound(Errors.single(s"Not found: '${req.uri}'.").asJson, noCache)

class Server[F[_]: Async] extends WebSyntax[F] with ScalatagsEncoders:
  AppLogger.init()
  val log = AppLogger(getClass)
  log.info("Starting app...")
  val html: Html = Html.default

  private val routes = HttpRoutes.of[F]:
    case GET -> Root =>
      Ok(IndexResponse.default.asJson)
    case GET -> Root / "html" =>
      Ok(html.index)
  private val httpApp = GZip:
    HSTS:
      orNotFound:
        Router("/" -> routes, "/assets" -> StaticService[F].routes)
  val server =
    EmberServerBuilder
      .default[F]
      .withHttp2
      .withHost(host"0.0.0.0")
      .withPort(port"9000")
      .withHttpApp(httpApp)
      .withShutdownTimeout(1.millis)
      .withErrorHandler(ErrorHandler[F].partial)
      .build

  def orNotFound(rs: HttpRoutes[F]): Kleisli[F, Request[F], Response[F]] =
    Kleisli: req =>
      rs.run(req).getOrElseF(notFound(req))

object Server extends IOApp:
  override def run(args: List[String]): IO[ExitCode] =
    Server[IO].server
      .use(_ => IO.never)
      .as(ExitCode.Success)
