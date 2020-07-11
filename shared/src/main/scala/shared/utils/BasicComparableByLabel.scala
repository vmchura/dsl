package shared.utils
import upickle.default.{ macroRW, ReadWriter => RW }

case class BasicComparableByLabel(stringLabel: String,id: String) extends ComparableByLabel
object BasicComparableByLabel {
  implicit val rw: RW[BasicComparableByLabel] = macroRW
}
