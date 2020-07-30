package shared

import scala.scalajs.js

@js.native
trait PlayCall[O] extends js.Object {
  /**
   * Call this entry point with AJAX, using the default settings.
   */
  def ajax(): js.Dynamic = js.native

  /**
   * The method of this entry point -- "GET", "POST" or whatever. Known in jQuery as "type".
   */
  def method: String = js.native

  /**
   * Synonym for method.
   */
  def `type`: String = js.native

  /**
   * The relative URL of this call.
   */
  def url: String = js.native

  /**
   * The absolute URL of this call.
   */
  def absoluteURL(): String = js.native

}