package teamsystem

import com.thoughtworks.binding.Binding
import com.thoughtworks.binding.Binding.Vars

import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}
import org.scalajs.dom
import org.scalajs.dom.raw.{HTMLButtonElement, HTMLDivElement, HTMLInputElement}
import org.lrng.binding.html
import org.scalajs.dom.html.Table
import scala.xml.Elem

@JSExportTopLevel("TeamsReplay")
object TeamsReplay {
  private val uploadComponents = Vars.empty[UploadTeamReplayComponent]
  implicit def elem2TableIntellIJ(e: Elem): Binding[Table] = ???

  @html
  private val tableContent: Binding[Table] = <table class="table">
    <thead>
      <tr>
        <th scope="col">Replay</th>
        <th scope="col">Primer jugador</th>
        <th scope="col">Segundo jugador</th>
        <th scope="col">Estado</th>
      </tr>
    </thead>
    <tbody>
      {
    for (component <- uploadComponents) yield {
      { component.render() }
    }
  }
    </tbody>
  </table>

  def handleUpload(
      inputFile: HTMLInputElement
  ): Unit = {
    uploadComponents.value.clear()
    uploadComponents.value.appendAll((0 until inputFile.files.length).map { i =>
      new UploadTeamReplayComponent(inputFile.files(i))
    })

  }
  @JSExport("init")
  def init(buttonID: String, containerID: String, inputFileID: String): Unit = {
    val button =
      dom.document.getElementById(buttonID).asInstanceOf[HTMLButtonElement]
    val container =
      dom.document.getElementById(containerID).asInstanceOf[HTMLDivElement]
    val fileInput =
      dom.document.getElementById(inputFileID).asInstanceOf[HTMLInputElement]

    fileInput.onchange = _ => handleUpload(fileInput)
    button.onclick = _ => fileInput.click()
    html.render(container, tableContent)

  }
}
