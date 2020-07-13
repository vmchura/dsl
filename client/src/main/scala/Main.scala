import org.scalajs.dom
import org.scalajs.dom.html.Input
import org.scalajs.dom.raw.HTMLFormElement

import scala.scalajs.js.annotation.JSExportTopLevel

@JSExportTopLevel("Main")
object Main {

  def main(args: Array[String]): Unit = {
    ()
  }
  def findTokenValue(): String = {
    val formElement = dom.document.getElementById("myForm").asInstanceOf[HTMLFormElement]
    val inputelementCollection = formElement.getElementsByTagName("input")
    val inputelement = inputelementCollection.namedItem("csrfToken").asInstanceOf[Input]
    inputelement.value
  }
}
