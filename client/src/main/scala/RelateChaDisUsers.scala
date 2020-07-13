import shared.UtilParser
import shared.utils.{BasicComparableByLabel, Util}

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}
import scala.util.{Failure, Success}
import upickle.default.read
import com.thoughtworks.binding.Binding.{Var, Vars}
import com.thoughtworks.binding.{Binding, FutureBinding}
import com.thoughtworks.binding.Binding
import Binding._
import org.lrng.binding.html
import html.NodeBinding
import org.scalajs.dom.ext.Ajax
import org.scalajs.dom.raw._
import upickle.default.write

import scala.scalajs.js
import scala.scalajs.js.JSON
@JSExportTopLevel("RelateChaDisUsers")
class RelateChaDisUsers (firstParam: String, secondParam: String, container: String) {
  private val first = read[Seq[BasicComparableByLabel]](UtilParser.safeString2Json(firstParam))
  private val second = read[Seq[BasicComparableByLabel]](UtilParser.safeString2Json(secondParam))

  @html
  private def pairComponent(f: BasicComparableByLabel) = {
    val selected = second.maxBy(s => Util.LCSLength(f.stringLabelNormalized,s.stringLabelNormalized))
    <tr>
      <td>{f.stringLabel}</td>
      <td>{selected.stringLabel}</td>
      <td>
        <button onclick={_: Event => postRelation(f,selected)}>Submit</button>
      </td>
    </tr>
  }

  @html
  private val p = <table>
    {first.map(pairComponent)}
  </table>

  @JSExport("showPanel")
  def showPanel(): Unit = {
    val containerComponent = org.scalajs.dom.document.getElementById(container)
    html.render(containerComponent, p)
  }

  def postRelation(f: BasicComparableByLabel, s: BasicComparableByLabel): Future[Either[String,Boolean]] = {
    val formData = new FormData()
    formData.append("challonge",write(f))
    formData.append("discord",write(s))
    println(formData)
    val request = Ajax.post("/participant/update", headers = Map("Csrf-Token" -> Main.findTokenValue()), data = formData)
    println("waiing")
    request.map{ response =>
      println(response)
      try {

        JSON.parse(response.responseText) match {
          case json: js.Dynamic => Right(read[Boolean](json.response.toString))
          case _ => Left("Response can not parse it as JSON: " + response.responseText)
        }
      } catch {
        case e: Throwable => Left("ERROR describing JSON?: " + e.toString)
      }
    }
  }
}
