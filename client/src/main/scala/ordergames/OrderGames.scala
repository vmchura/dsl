package ordergames

import com.thoughtworks.binding.Binding.Vars
import org.lrng.binding.html
import org.scalajs.dom.raw.{HTMLFormElement, KeyboardEvent}
import org.scalajs.dom.{Event, document}
import shared.UtilParser
import shared.models.ReplayRecordShared
import upickle.default.read

import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}
@JSExportTopLevel("OrderGames")
class OrderGames(tournamentName: String, matchesString: String,  container: String){
  private val matches = Vars[GameAsItem](read[Seq[ReplayRecordShared]](UtilParser.safeString2Json(matchesString)).zipWithIndex.map{case (g,i) => GameAsItem(g,i)}: _*)
  val organizableMatches = new Organizable(matches)
  val bof = new BestOfBlock("bof")
  @html
  private val module = {
    <ul class="list-group">
      {for (g <- organizableMatches.sortedElements) yield {

      <li class="list-group-item">
        <div class="row-gi">
          <input type="hidden" name={s"replayID[${g.ordering}]"} id={s"replayID_${g.ordering}"} value={g.replayRecordShared.replayID.toString} />
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
    <button class="btn btn-primary" type="submit" disabled={bof.numberOfGames.bind.isEmpty}>
      Ordenar y crear carpetas
    </button>
  }


  document.getElementById("formOrderGames").asInstanceOf[HTMLFormElement].onkeypress = (ae: KeyboardEvent) => {
    if (ae.keyCode == 13) {
      ae.preventDefault()
      false
    }else{
      true
    }
  }

  @JSExport("showPanel")
  def showPanel(): Unit = {
    val containerComponent = org.scalajs.dom.document.getElementById(container)
    html.render(containerComponent, module)
  }
}
