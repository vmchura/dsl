import shared.UtilParser
import shared.utils.BasicComparableByLabel

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}
import scala.util.{Failure, Success}
import upickle.default.read


import com.thoughtworks.binding.Binding.{ Var, Vars }
import com.thoughtworks.binding.{ Binding, FutureBinding }
import com.thoughtworks.binding.Binding, Binding._
import org.lrng.binding.html, html.NodeBinding
import org.scalajs.dom.raw._

@JSExportTopLevel("RelateChaDisUsers")
class RelateChaDisUsers (firstParam: String, secondParam: String, container: String) {
  val first = read[Seq[BasicComparableByLabel]](UtilParser.safeString2Json(firstParam))
  val second = read[Seq[BasicComparableByLabel]](UtilParser.safeString2Json(secondParam))
  println(first)
  println(second)
  @html
  val p = <table>
    {first.map(p => <tr>{p.stringLabel}</tr>)}
  </table>

  @JSExport("showPanel")
  def showPanel(): Unit = {
    val containerComponent = org.scalajs.dom.document.getElementById(container)
    html.render(containerComponent, p)
  }
}
