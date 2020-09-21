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
import shared.models.ReplayRecordShared
@JSExportTopLevel("OrderGames")
class OrderGames(tournamentName: String, matchesString: String,  container: String){
  private val matches = read[Seq[ReplayRecordShared]](UtilParser.safeString2Json(matchesString))

  @html
  val module = {
    <div>
      {matches.mkString(" ")}
    </div>
  }

  @JSExport("showPanel")
  def showPanel(): Unit = {
    val containerComponent = org.scalajs.dom.document.getElementById(container)
    html.render(containerComponent, module)
  }
}
