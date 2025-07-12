package com.malliina.server

import com.malliina.live.LiveReload
import org.http4s.Uri
import scalatags.Text.all.*
import scalatags.text.Builder
import com.malliina.server.Html.given

object Html:
  val default =
    Html(
      Seq("main.js"),
      Seq("main.css"),
      HashedAssetsSource
    )

  given AttrValue[Uri] =
    (t: Builder, a: Attr, v: Uri) =>
      t.setAttr(a.name, Builder.GenericAttrValueSource(v.renderString))

class Html(scripts: Seq[String], cssFiles: Seq[String], assets: AssetsSource):
  def index = html(lang := "en")(
    head(
      meta(charset := "utf-8"),
      tag("title")("Example"),
      meta(name := "viewport", content := "width=device-width, initial-scale=1.0"),
      link(
        rel := "shortcut icon",
        tpe := "image/png"
//        href := inlineOrAsset(FileAssets.img.jag_16x16_png)
      ),
      cssFiles.map(file => cssLink(file)),
      scripts.map(js => deferredJsPath(js)),
      script(src := LiveReload.script)
    ),
    body(
      p("Hi!"),
      button(
        tpe := "button",
        cls := "btn btn-lg btn-danger",
        data("bs-toggle") := "popover",
        data("bs-title") := "Popover title",
        data("bs-content") := "Content here."
      )("Click to toggle popover"),
      div(cls := "demo")("Hmm"),
      div(cls := "pic")("")
    )
  )

  private def deferredJsPath(path: String) =
    script(`type` := "application/javascript", src := assets.at(path), defer)

  private def cssLink(path: String, more: Modifier*) =
    link(rel := "stylesheet", href := assets.at(path), more)
