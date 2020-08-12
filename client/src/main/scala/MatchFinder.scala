import org.lrng.binding.html
import org.scalajs.dom.html.Div
import com.thoughtworks.binding.Binding.Var
import org.lrng.binding.html.NodeBinding
import org.scalajs.dom.raw.{HTMLInputElement, HTMLTableRowElement}
import org.scalajs.dom.document

import scala.collection.mutable.ArrayBuffer
import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}


@JSExportTopLevel("MatchFinder")
object MatchFinder {

  private val roundName = Var[String]("")
  private val userName = Var[String]("")


  case class MatchSimpleRow(id: String, round: String, player1: String, player2: String)
  private val matchesFromDocument = ArrayBuffer.empty[MatchSimpleRow]
  type FilterType = MatchSimpleRow => Boolean
  def filterTable(): Unit = {
    def roundFilter: FilterType = msr => if(roundName.value.nonEmpty) msr.round.toLowerCase.contains(roundName.value.toLowerCase) else true
    def userFilter: FilterType = msr => if(userName.value.nonEmpty)
      msr.player1.toLowerCase.contains(userName.value.toLowerCase) ||
        msr.player2.toLowerCase.contains(userName.value.toLowerCase)
    else true

    val globalFilter: FilterType = msr => roundFilter(msr) && userFilter(msr)

    val (show,hide) = matchesFromDocument.partition(globalFilter)
    hide.foreach{ msr =>
      val element = document.getElementById(msr.id)
      element.classList.remove("match-hidden")
      element.classList.remove("match-shown")
      element.classList.add("match-hidden")
    }
    show.foreach{ msr =>
      val element = document.getElementById(msr.id)
      element.classList.remove("match-hidden")
      element.classList.remove("match-shown")
      element.classList.add("match-shown")
    }
  }

  @html
  private val finderByRoundMatch = {
    val inputElement: NodeBinding[HTMLInputElement] =  <input type="text" class="form-control" placeholder="Ronda" data:aria-label="Ronda" data:aria-describedby="basic-addon-ronda1"/>

    inputElement.value.oninput = _ => {
      val newText = inputElement.value.value
      roundName.value = newText
      filterTable()
    }

    val inputComponent: NodeBinding[Div] =   <div class="input-group mb-3">
      <span class="input-group-text" id="basic-addon-ronda1">Ronda</span>
      {inputElement}
    </div>

    inputComponent
  }
  @html
  private val finderByUser = {
    val inputElement: NodeBinding[HTMLInputElement] =  <input type="text" class="form-control" placeholder="Nombre de usuario" data:aria-label="Nombre de usuario" data:aria-describedby="basic-addon-jugador1"/>

    inputElement.value.oninput = _ => {
      val newText = inputElement.value.value
      userName.value = newText
      filterTable()
    }

    val inputComponent: NodeBinding[Div] =  <div class="input-group mb-3">
      <span class="input-group-text" id="basic-addon-jugador1">Nombre de usuario</span>
      {inputElement}
    </div>

    inputComponent
  }

  @html
  private val component = <div class="container">
    <div class="row justify-content-md-center">
      <div class="col col-lg-3">
        {finderByRoundMatch}
      </div>
      <div class="col col-lg-3">
        {finderByUser}
      </div>
    </div>
  </div><div>


  </div>


  @JSExport("init")
  def init(containerID: String): Unit ={
    val containerComponent = org.scalajs.dom.document.getElementById(containerID)
    html.render(containerComponent, component)
    val trElements = document.getElementsByTagName("tr")

    val trs: Seq[HTMLTableRowElement] = (0 until trElements.length).map(i => trElements.item(i).asInstanceOf[HTMLTableRowElement]).filter(_.id.nonEmpty)
    matchesFromDocument.appendAll(trs.map(row => {
      val tds = row.getElementsByTagName("td")
      val msr = MatchSimpleRow(row.id, tds(0).innerHTML, tds(1).innerHTML, tds(2).innerHTML)
      msr
    }))
  }
}
