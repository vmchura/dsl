package ordergames
import org.lrng.binding.html
import com.thoughtworks.binding.Binding.Var
import org.lrng.binding.html.NodeBinding
import org.scalajs.dom.document
import org.scalajs.dom.raw.HTMLInputElement

class BestOfBlock(inputID: String) {
  implicit def makeIntellijHappy(x: scala.xml.Node): NodeBinding[HTMLInputElement] = throw new NotImplementedError()
  val numberOfGames: Var[Option[Int]] = Var(None)
  private val input = document.getElementById(inputID).asInstanceOf[HTMLInputElement]
  def updateInputValue(): Unit = {
    numberOfGames.value = input.value.toIntOption.flatMap(i => if(i > 0 && i<=11 && i%2==1) Some(i) else None)
  }
  input.oninput = _ =>  updateInputValue()
  updateInputValue()
}
