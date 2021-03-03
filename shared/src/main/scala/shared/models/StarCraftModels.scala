package shared.models
import upickle.default.{macroRW, ReadWriter => RW}

object StarCraftModels {

  trait SCRace {
    def str: String
  }

  case object Protoss extends SCRace {
    override def str: String = "protoss"
  }

  case object Terran extends SCRace {
    override def str: String = "terran"
  }

  case object Zerg extends SCRace {
    override def str: String = "zerg"
  }

  case object Random extends SCRace {
    override def str: String = "random"
  }

  case class SCPlayer(smurf: String, race: SCRace)
  object SCRace {
    def apply(str: String): SCRace =
      str match {
        case "protoss" => Protoss
        case "terran"  => Terran
        case "zerg"    => Zerg
        case _         => Random
      }
    implicit val rw: RW[SCRace] = upickle.default
      .readwriter[String]
      .bimap[SCRace](
        x => x.str,
        SCRace.apply
      )
  }
  object SCPlayer {
    implicit val rw: RW[SCPlayer] = macroRW
  }

  trait SCGameMode {
    def mapName: String
    def startTime: String
  }

  case class OneVsOne(
      winner: SCPlayer,
      loser: SCPlayer,
      mapName: String,
      startTime: String
  ) extends SCGameMode
  object OneVsOne {
    implicit val rw: RW[OneVsOne] = macroRW
  }

  case class ManyVsMany(
      winners: Seq[SCPlayer],
      losers: Seq[SCPlayer],
      mapName: String,
      startTime: String
  ) extends SCGameMode

  case class InvalidSCGameMode(
      participants: Seq[SCPlayer]
  ) extends SCGameMode {
    override val mapName: String = "???"

    override val startTime: String = "???"
  }

  trait SCMatchMode

  case object Melee extends SCMatchMode
  case object TopVsBottom extends SCMatchMode
  case object OneVsOneMode extends SCMatchMode
  case object DangerMode extends SCMatchMode
  case object UnknownMode extends SCMatchMode

}
