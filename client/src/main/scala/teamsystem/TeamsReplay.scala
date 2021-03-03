package teamsystem

import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}
import org.scalajs.dom
import org.scalajs.dom.raw.{HTMLButtonElement, HTMLDivElement}
@JSExportTopLevel("TeamsReplay")
object TeamsReplay {
  def handleUpload(container: HTMLDivElement): Unit = {
    println("Clicked")
  }
  @JSExport("init")
  def init(buttonID: String, containerID: String): Unit = {
    val button =
      dom.document.getElementById(buttonID).asInstanceOf[HTMLButtonElement]
    val container =
      dom.document.getElementById(buttonID).asInstanceOf[HTMLDivElement]

    button.onclick = _ => handleUpload(container)
  }
}
