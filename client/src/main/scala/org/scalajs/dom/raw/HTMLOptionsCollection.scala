package org.scalajs.dom.raw

import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobal

@js.native
@JSGlobal
abstract class HTMLOptionsCollection extends HTMLCollection {
  def add(option: HTMLOptionElement): Unit = js.native
  def remove(index: Int): Unit = js.native
}
