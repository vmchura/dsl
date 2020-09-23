package ordergames

import com.thoughtworks.binding.Binding.Vars
import org.lrng.binding.html
import org.scalajs.dom.Event
import shared.UtilParser
import shared.models.ReplayRecordShared
import upickle.default.read

import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}
@JSExportTopLevel("OrderGames")
class OrderGames(tournamentName: String, matchesString: String,  container: String){
  private val matches = Vars[GameAsItem](read[Seq[ReplayRecordShared]](UtilParser.safeString2Json(matchesString)).zipWithIndex.map{case (g,i) => GameAsItem(g,i+1)}: _*)
  val organizableMatches = new Organizable(matches)

  @html
  val module = {
    <ul class="list-group">
      {for (g <- organizableMatches.sortedElements) yield {
      <li class="list-group-item">
        <div class="row-gi">
          <div>
            {g.content}
          </div>
          <button
          type="button"
          class="btn btn-primary"
          onclick={_: Event => organizableMatches.assignLowerOrder(g)}>
            UP
          </button>
          <button
          type="button"
          class="btn btn-primary"
          onclick={_: Event => organizableMatches.assignHigherOrder(g)}>
            DOWN
          </button>
        </div>
      </li>
      }}
    </ul>
  }

  @JSExport("showPanel")
  def showPanel(): Unit = {
    val containerComponent = org.scalajs.dom.document.getElementById(container)
    html.render(containerComponent, module)
  }
}
