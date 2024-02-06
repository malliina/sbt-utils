package com.malliina.server

import com.malliina.live.LiveReload
import org.http4s.Uri
import scalatags.Text.all.*
import scalatags.text.Builder
import com.malliina.server.Html.given

object Html:
  val default =
    Html(
      if BuildInfo.isProd then Seq("frontend.js")
      else Seq("frontend.js", "client-loader.js", "main.js"),
      Seq("frontend.css", "styles.css"),
      DirectAssets
    )

  given AttrValue[Uri] =
    (t: Builder, a: Attr, v: Uri) =>
      t.setAttr(a.name, Builder.GenericAttrValueSource(v.renderString))

class Html(scripts: Seq[String], cssFiles: Seq[String], assets: AssetsSource):
  def index = html(lang := "en")(
    head(
      meta(charset := "utf-8"),
      cssFiles.map(file => cssLink(assets.at(file))),
      scripts.map(js => deferredJsPath(js)),
      script(src := LiveReload.script)
    ),
    body(
      p("Hi!"),
      button(
        `type` := "button",
        `class` := "btn btn-lg btn-danger",
        data("bs-toggle") := "popover",
        data("bs-title") := "Popover title",
        data("bs-content") := "Content here."
      )("Click to toggle popover")
    )
  )

  private def deferredJsPath(path: String) =
    script(`type` := "application/javascript", src := assets.at(path), defer)

  private def cssLink[V: AttrValue](url: V, more: Modifier*) =
    link(rel := "stylesheet", href := url, more)
