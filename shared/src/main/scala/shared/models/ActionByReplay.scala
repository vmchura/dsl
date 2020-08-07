package shared.models

import shared.models.ActionBySmurf.ActionBySmurf
import upickle.default.{macroRW, ReadWriter => RW}


object ActionBySmurf extends Enumeration {
  protected case class Val(str: String) extends super.Val
  import scala.language.implicitConversions
  implicit def valueToStrVal(x: Value): Val = x.asInstanceOf[Val]

  type ActionBySmurf = Value
  val CompletelyDefined: Val = Val("CompletelyDefined")
  val SmurfsEmpty: Val = Val("SmurfsEmpty")
  val ImpossibleToDefine: Val = Val("ImpossibleToDefine")

  implicit val rw: RW[ActionBySmurf] = upickle.default.readwriter[String].bimap[ActionBySmurf](
    x => x.str,
    {
      case "CompletelyDefined" => CompletelyDefined
      case "SmurfsEmpty" => SmurfsEmpty
      case "ImpossibleToDefine" => ImpossibleToDefine
      case x =>
        println(x)
        ImpossibleToDefine
    }
  )
}
case class ActionByReplay(defined: Boolean,
                          player1: Option[String],
                          player2: Option[String],
                          actionBySmurf: ActionBySmurf,
                          winner: Int)
object ActionByReplay{
  implicit val rw: RW[ActionByReplay] = macroRW
}
