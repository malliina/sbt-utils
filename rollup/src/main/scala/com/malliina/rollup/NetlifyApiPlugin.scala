package com.malliina.rollup

import cats.effect.unsafe.implicits.global
import com.malliina.netlify.Netlify
import com.malliina.netlify.Netlify.SiteId
import com.malliina.rollup.CommonKeys.{assetsRoot, build, deploy}
import com.malliina.values.{AccessToken, ErrorMessage}
import sbt.Keys.streams
import sbt.{AutoPlugin, Plugins, Setting}

object NetlifyApiPlugin extends AutoPlugin {
  override def requires: Plugins = LoggingPlugin

  override def projectSettings: Seq[Setting[?]] = Seq(
    deploy := {
      val io = Netlify
        .deploy[cats.effect.IO](
          fs2.io.file.Path.fromNioPath(assetsRoot.value),
          env[SiteId]("NETLIFY_SITE_ID"),
          env[AccessToken]("NETLIFY_AUTH_TOKEN")
        )
      val id = io.unsafeRunSync()
      streams.value.log.info(s"Deployment '$id' done.")
    },
    deploy := deploy.dependsOn(build).value
  )

  private def env[T](key: String)(implicit r: com.malliina.values.Readable[T]): T = {
    val result = for {
      str <- sys.env.get(key).toRight(ErrorMessage(s"Please define environment variable '$key'."))
      t <- r.read(str)
    } yield t
    result.fold(err => sys.error(err.message), identity)
  }
}
