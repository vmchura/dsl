package models

trait SCRace
case object Protoss extends SCRace
case object Terran extends SCRace
case object Zerg extends SCRace
case object Random extends SCRace

case class SCPlayer(smurf: String, race: SCRace)

trait SCGameMode
case class OneVsOne(winner: SCPlayer, loser: SCPlayer) extends SCGameMode
case class ManyVsMany(winners: Seq[SCPlayer], losers: Seq[SCPlayer])
    extends SCGameMode
case class InvalidSCGameMode(participants: Seq[SCPlayer]) extends SCGameMode

trait SCMatchMode
case object Melee extends SCMatchMode
case object TopVsBottom extends SCMatchMode
case object OneVsOneMode extends SCMatchMode
case object UnknownMode extends SCMatchMode
