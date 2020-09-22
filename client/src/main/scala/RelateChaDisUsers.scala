import shared.UtilParser
import shared.utils.{BasicComparableByLabel, Util}

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}
import upickle.default.read
import com.thoughtworks.binding.{Binding, FutureBinding}
import com.thoughtworks.binding.Binding.{Var, Vars}
import org.lrng.binding.html
import org.scalajs.dom.ext.Ajax
import org.scalajs.dom.raw._
import upickle.default.write

import scala.scalajs.js
import scala.scalajs.js.JSON
import scala.util.{Failure, Success}
import org.scalajs.dom.window
import org.scalajs.dom.document
@JSExportTopLevel("RelateChaDisUsers")
class RelateChaDisUsers (firstParam: String, secondParam: String, container: String) {
  private val first = read[Seq[BasicComparableByLabel]](UtilParser.safeString2Json(firstParam))
  private val second = read[Seq[BasicComparableByLabel]](UtilParser.safeString2Json(secondParam))
  case class PairMatch(chauser: BasicComparableByLabel, disuser: Option[BasicComparableByLabel],
                       bindedCorrectly: FutureBinding[Either[String,Boolean]])
  private val pairsEnabled = Vars(first.map(f => PairMatch(f,None, FutureBinding(Future.successful(Right(false))))): _*)

  @html
  private def getMessageLinked(fut: FutureBinding[Either[String,Boolean]]) = Binding{
    (fut.bind match {
      case None                         =>  <p>Wait</p>
      case Some(Success(Left(error)))   =>  <p>{error}</p>
      case Some(Success(Right(true)))   =>  <p>Relacionado!</p>
      case Some(Success(Right(false)))  =>  <p>Error!!</p>
      case Some(Failure(exception))     =>  <p>Error!! {exception.toString}</p>
    }).bind
  }

  @html
  private def pairComponent(pm: PairMatch) = {
    val listThisMatch = second.sortBy(s => Util.LCSLength(pm.chauser.stringLabelNormalized,s.stringLabelNormalized)).reverse
    var selected: Option[BasicComparableByLabel] = listThisMatch.headOption

    <tr>
      <td>{pm.chauser.stringLabel}</td>
      <td>{pm.disuser.map(_.stringLabel).getOrElse("¿?")}</td>
      <td>

        <select class="form-select" onchange={e: Event => {
          val selectionValue = e.currentTarget.asInstanceOf[HTMLSelectElement]
          val value = selectionValue.value
          second.find(_.id.equals(value)) match {
            case Some(selectedValue) => selected = Some(selectedValue)
            case None => window.alert("error en la aplicación, repórtalo!!!")

          }
        }
        }>
        {
          listThisMatch.map{ o =>
            <option value={o.id}>{o.stringLabel}</option>
          }
        }

        </select>

      </td>
      <td>
        <button onclick={_: Event => {
          selected match {
            case Some(selection) => {
              buildRelation(pm,selection)
            }
            case None => window.alert("Please, select value")
          }
        }}>Submit</button>
      </td>
      <td>{getMessageLinked(pm.bindedCorrectly)}
      </td>
    </tr>
  }

  @html
  private val p = <div class="container">
    <table class="table table-hover">
    {pairsEnabled.map(pairComponent)}
  </table></div>

  @JSExport("showPanel")
  def showPanel(): Unit = {
    val containerComponent = org.scalajs.dom.document.getElementById(container)
    html.render(containerComponent, p)
  }

  def postRelation(f: BasicComparableByLabel, s: BasicComparableByLabel): Future[Either[String,Boolean]] = {
    val formData = new FormData()
    formData.append("challonge",write(f))
    formData.append("discord",write(s))
    val request = Ajax.post("/participant/update", headers = Map("Csrf-Token" -> Main.findTokenValue()), data = formData)
    request.map{ response =>
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
  def buildRelation(pm: PairMatch, selected: BasicComparableByLabel): Unit = {

    val indx = pairsEnabled.value.indexOf(pm)
    if(indx >= 0){
      val fut = FutureBinding(postRelation(pm.chauser, selected))
      pairsEnabled.value.update(indx,pm.copy(disuser = Some(selected),bindedCorrectly = fut))
    }
  }
}
