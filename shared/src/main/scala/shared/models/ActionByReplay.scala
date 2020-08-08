package shared.models

import shared.models.ActionBySmurf.ActionBySmurf
import upickle.default.{macroRW, ReadWriter => RW}


object ActionBySmurf extends Enumeration {
  protected case class Val(str: String) extends super.Val
  import scala.language.implicitConversions
  implicit def valueToStrVal(x: Value): Val = x.asInstanceOf[Val]

  type ActionBySmurf = Value
  val Correlated1d1rDefined: Val = Val("Correlated1d1rDefined")
  val Correlated2d2rDefined: Val = Val("Correlated2d2rDefined")
  val Correlated1d2rDefined: Val = Val("Correlated1d2rDefined")
  val Correlated2d1rDefined: Val = Val("Correlated2d1rDefined")
  val CorrelatedParallelDefined: Val = Val("CorrelatedParallelDefined")
  val CorrelatedCruzadoDefined: Val = Val("CorrelatedCruzadoDefined")
  val CorrelatedBoth: Val = Val("CorrelatedBoth")
  val SmurfsEmpty: Val = Val("SmurfsEmpty")
  val ImpossibleToDefine: Val = Val("ImpossibleToDefine")

  implicit val rw: RW[ActionBySmurf] = upickle.default.readwriter[String].bimap[ActionBySmurf](
    x => x.str,
    {
      case "Correlated1d1rDefined" => Correlated1d1rDefined
      case "Correlated2d2rDefined" => Correlated2d2rDefined
      case "Correlated1d2rDefined" => Correlated1d2rDefined
      case "Correlated2d1rDefined" => Correlated2d1rDefined
      case "CorrelatedParallelDefined" => CorrelatedParallelDefined
      case "CorrelatedCruzadoDefined" => CorrelatedCruzadoDefined
      case "CorrelatedBoth" => CorrelatedBoth
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
