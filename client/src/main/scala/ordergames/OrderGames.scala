package ordergames

import com.thoughtworks.binding.Binding.Vars
import org.lrng.binding.html
import shared.UtilParser
import shared.models.ReplayRecordShared
import upickle.default.read

import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}
@JSExportTopLevel("OrderGames")
class OrderGames(tournamentName: String, matchesString: String,  container: String){
  private val matches = Vars[GameAsItem](read[Seq[ReplayRecordShared]](UtilParser.safeString2Json(matchesString)).zipWithIndex.map{case (g,i) => GameAsItem(g,i)}: _*)

  @html
  val module = {
    <div>
      {for(g <- matches) yield{
          g.content
        }
      }
    </div>
  }

  @JSExport("showPanel")
  def showPanel(): Unit = {
    val containerComponent = org.scalajs.dom.document.getElementById(container)
    html.render(containerComponent, module)
  }
}
