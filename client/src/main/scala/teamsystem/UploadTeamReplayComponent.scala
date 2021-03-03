package teamsystem

import com.thoughtworks.binding.Binding
import com.thoughtworks.binding.Binding.Var
import org.lrng.binding.html
import org.scalajs.dom.raw.{File, HTMLTableRowElement}
import shared.models.StarCraftModels.OneVsOne

import scala.xml.Elem

class UploadTeamReplayComponent(file: File) {
  private val rowClass = Var[String]("table-light")
  implicit def elem2TableRow(e: Elem): Binding[HTMLTableRowElement] = ???
  private val messageError = Var[Option[String]](None)
  private val oneVsOne = Var[Option[OneVsOne]](None)

  @html
  def render(): Binding[HTMLTableRowElement] = {
    <tr class={rowClass.bind}>
      <td> {file.name} </td>
    </tr>
  }
}
