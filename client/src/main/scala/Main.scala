import org.scalajs.dom
import org.scalajs.dom.html.Input
import org.scalajs.dom.raw.HTMLFormElement

import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}

@JSExportTopLevel("Main")
object Main {

  def main(args: Array[String]): Unit = {

  }
  @JSExport("init")
  def init(): Unit = {

    ReplayUpdater.init()

  }
  def findTokenValue(): String = {
    val formElement = dom.document.getElementById("myForm").asInstanceOf[HTMLFormElement]
    val inputelementCollection = formElement.getElementsByTagName("input")
    val inputelement = inputelementCollection.namedItem("csrfToken").asInstanceOf[Input]
    inputelement.value
  }
}
