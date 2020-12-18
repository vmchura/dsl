import com.thoughtworks.binding.Binding
import org.lrng.binding.html
import org.scalajs.dom.Node
import org.scalajs.dom.html.Div

class ReplayUploaderByPlayer(
    val fieldDiv: Div,
    val player1: String,
    val player2: String,
    val discord1: String,
    val discord2: String
) extends ReplayUploader {
  import ReplayUploader._

  @html
  override def hiddenInputValuesSelected: Binding[Node] = <div></div>

  override def nameForInput: String = "mySmurf"

  override def prefixToUserNameIfEmpty: String = "Hola"

  override def jugarConjugation: String = "jugaste"
}
object ReplayUploaderByPlayer {
  def apply(
      fieldDiv: Div,
      player1: String,
      player2: String,
      discord1: String,
      discord2: String
  ): ReplayUploaderByPlayer =
    new ReplayUploaderByPlayer(fieldDiv, player1, player2, discord1, discord2)
}
