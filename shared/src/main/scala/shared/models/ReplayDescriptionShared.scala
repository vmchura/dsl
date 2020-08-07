package shared.models

import upickle.default.{ macroRW, ReadWriter => RW }

case class ReplayDescriptionShared(player1: String, player2: String, winner: Int)
object ReplayDescriptionShared{
  implicit val rw: RW[ReplayDescriptionShared] = macroRW
}