package com.malliina.client

import org.scalajs.dom
import org.scalajs.dom.document

import scala.scalajs.js
import scala.scalajs.js.Dynamic.literal
import scala.scalajs.js.annotation.JSImport

object Frontend:
  val bootstrapCss = BootstrapCss
  val bootstrapJs = Bootstrap
  val popperJs = Popper

  def main(args: Array[String]): Unit =
    val popovers = document
      .querySelectorAll("[data-bs-toggle='popover']")
      .map: e =>
        new Popover(e, PopoverOptions.click)
    println(s"Hi, got ${popovers.size} popover(s)...")

@js.native
@JSImport("@popperjs/core", JSImport.Namespace)
object Popper extends js.Object

@js.native
trait PopoverOptions extends js.Object:
  def trigger: String

object PopoverOptions:
  def apply(trigger: String): PopoverOptions =
    literal(trigger = trigger).asInstanceOf[PopoverOptions]

  val click = apply("click")
  val focus = apply("focus")
  val manual = apply("manual")

@js.native
@JSImport("bootstrap", JSImport.Namespace)
object Bootstrap extends js.Object

@js.native
@JSImport("bootstrap", "Popover")
class Popover(e: dom.Element, options: PopoverOptions) extends js.Any:
  def hide(): Unit = js.native
  def show(): Unit = js.native

@js.native
@JSImport("bootstrap/dist/css/bootstrap.min.css", JSImport.Namespace)
object BootstrapCss extends js.Object
