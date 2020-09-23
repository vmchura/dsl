package ordergames
import org.lrng.binding.html
import com.thoughtworks.binding.Binding.Var
import org.lrng.binding.html.NodeBinding
import org.scalajs.dom.raw.HTMLInputElement

class BestOfBlock() {
  implicit def makeIntellijHappy(x: scala.xml.Node): NodeBinding[HTMLInputElement] = throw new NotImplementedError()
  val numberOfGames: Var[Option[Int]] = Var(Some(3))
  @html
  val textInput: NodeBinding[HTMLInputElement] = {
    val input: NodeBinding[HTMLInputElement] = <input type="text" class="form-control" data:aria-label="Best Of" value="3"/>
    input.value.onchange = _ => numberOfGames.value = input.value.value.toIntOption.flatMap(i => if(i > 0 && i<=11 && i%2==1) Some(i) else None)
    input
  }


}
