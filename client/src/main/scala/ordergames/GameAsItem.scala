package ordergames
import org.lrng.binding.html
import com.thoughtworks.binding.Binding.Var
import org.lrng.binding.html.NodeBinding
import org.scalajs.dom.raw.HTMLInputElement
import shared.models.ReplayRecordShared

case class GameAsItem(replayRecordShared: ReplayRecordShared, order: Int) {

  @html
  val content = <div>
    <span class="order-gi">{order.toString}</span>
    <span class="time-gi">{replayRecordShared.dateGame.getOrElse("???")}</span>
    <span class="nombre-gi">{replayRecordShared.nombreOriginal}</span>
  </div>
}
