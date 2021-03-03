package dslclient

import com.thoughtworks.binding.Binding
import org.lrng.binding.html
import org.scalajs.dom.Node
import org.scalajs.dom.html.Div

class ReplayUploaderByAdmin(
    val fieldDiv: Div,
    val player1: String,
    val player2: String,
    val discord1: String,
    val discord2: String
) extends ReplayUploader {
  import ReplayUploader._

  @html
  private def buildInput(parameter: Option[Array[String]]) = {
    parameter match {
      case Some(Array(d1, s1, d2, s2)) =>
        <div>
          <input type="hidden" name="bothIDsSmurfs[0]" value={d1}/>
          <input type="hidden" name="bothIDsSmurfs[1]" value={s1}/>
          <input type="hidden" name="bothIDsSmurfs[2]" value={d2}/>
          <input type="hidden" name="bothIDsSmurfs[3]" value={s2}/>
        </div>
      case _ => <div></div>
    }
  }

  @html
  override val hiddenInputValuesSelected: Binding[Node] =
    Binding {
      buildInput(discordIdsSmurf.bind).bind
    }

  override def nameForInput: String = "firstUserSmurf"

  override def prefixToUserNameIfEmpty: String = "Para"

  override def jugarConjugation: String = "jugó"
}
object ReplayUploaderByAdmin {
  def apply(
      fieldDiv: Div,
      player1: String,
      player2: String,
      discord1: String,
      discord2: String
  ): ReplayUploaderByAdmin =
    new ReplayUploaderByAdmin(fieldDiv, player1, player2, discord1, discord2)
}
