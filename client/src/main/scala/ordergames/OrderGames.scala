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
  val bof = new BestOfBlock()
  @html
  val module = {
    <div class="input-group mb-3">
      <span class="input-group-text" id="basic-addon1">Best of </span>
      {bof.textInput}
    </div>
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
    <button disabled={bof.numberOfGames.bind.isEmpty}>
      Ordenar y crear carpetas
    </button>
    <button>
      Marcar como secreto
    </button>
    <button>
      Liberar del secreto
    </button>
  }

  @JSExport("showPanel")
  def showPanel(): Unit = {
    val containerComponent = org.scalajs.dom.document.getElementById(container)
    html.render(containerComponent, module)
  }
}
