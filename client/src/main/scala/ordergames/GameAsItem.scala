package ordergames
import org.lrng.binding.html
import com.thoughtworks.binding.Binding.Var
import org.lrng.binding.html.NodeBinding
import org.scalajs.dom.raw.HTMLInputElement
import shared.models.ReplayRecordShared

case class GameAsItem(replayRecordShared: ReplayRecordShared, ordering: Int) extends WithOrdering[GameAsItem]{

  @html
  val content = <div>
    <span class="order-gi font-weight-bold">Game {ordering.toString}</span>
    <span class="time-gi">{replayRecordShared.dateGame.map(d => if(d.contains('T')) d.dropWhile(_ != 'T').tail else d ).getOrElse("???")}</span>
    <span class="name-gi font-weight-bold">{replayRecordShared.nombreOriginal}</span>
  </div>

  override def withNewOrder(order: Int): GameAsItem = copy(ordering = order)
}
