package ordergames
import org.lrng.binding.html
import com.thoughtworks.binding.Binding.Var
import org.lrng.binding.html.NodeBinding
import org.scalajs.dom.raw.HTMLInputElement

class BestOfBlock(text: Var[Option[Int]]) {
  implicit def makeIntellijHappy(x: scala.xml.Node): NodeBinding[HTMLInputElement] = throw new NotImplementedError()
  @html
  val textInput: NodeBinding[HTMLInputElement] = {
    val input: NodeBinding[HTMLInputElement] = <input type="text" class="form-control" data:aria-label="Best Of"/>
    input.value.onchange = _ => text.value = input.value.value.toIntOption
    input
  }

}
