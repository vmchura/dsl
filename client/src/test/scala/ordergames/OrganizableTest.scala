package ordergames

import com.thoughtworks.binding.Binding.Vars
import org.lrng.binding.html
import org.scalajs.dom.window
import utest._

import scala.scalajs.js
import org.scalajs.dom
import org.scalajs.dom.document
import org.scalajs.dom.ext._

object OrganizableTest  extends TestSuite{
  def tests = Tests {
    test("scala binding") {
      @html
      def comment = <div><!--my comment--></div>
      val div = document.createElement("div")
      html.render(div, comment)
      assert(div.innerHTML == "<div><!--my comment--></div>")
    }
    test("Organizable"){
      case class WO(ordering: Int, id: String) extends WithOrdering[WO] {
        override def withNewOrder(order: Int): WO = copy(ordering = order)
      }

      val elements = Vars[WO]((0 to 3).map(i => WO(i,('a'.toInt + i).toChar.toString)): _*)
      val e = new Organizable[WO](elements)
      assert(e.sortedElements.value.map(_.id).mkString("").equals("abcd"))
      e.assignHigherOrder(elements.value(2))
      assert(e.sortedElements.value.map(_.id).mkString("").equals("abdc"))
      e.assignHigherOrder(elements.value(0))
      assert(e.sortedElements.value.map(_.id).mkString("").equals("badc"))
      e.assignLowerOrder(elements.value(3))
      assert(e.sortedElements.value.map(_.id).mkString("").equals("bdac"))
    }
  }
}
