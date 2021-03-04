package shared.models

import shared.models.StarCraftModels.OneVsOne
import upickle.default.{macroRW, ReadWriter => RW}

import java.util.UUID

case class ReplayTeamID(id: UUID) extends AnyVal
object ReplayTeamID {
  implicit val rw: RW[ReplayTeamID] = macroRW
  def apply(): ReplayTeamID = new ReplayTeamID(UUID.randomUUID())
  def apply(id: UUID): ReplayTeamID = new ReplayTeamID(id)
}

case class ReplayTeamRecord(
    id: ReplayTeamID,
    replayMD5Hash: String,
    nombreOriginal: String,
    dateGame: String,
    dateUploaded: String,
    uploader: DiscordID
)
object ReplayTeamRecord {
  implicit val rw: RW[ReplayTeamRecord] = macroRW
}
case class ResultPending(replayTeamID: ReplayTeamID, onevsone: OneVsOne)
case class ResultSaved(
    replayTeamID: ReplayTeamID,
    onevsone: OneVsOne,
    winner: Option[DiscordID],
    loser: Option[DiscordID]
)
object ResultPending {
  implicit val rw: RW[ResultPending] = macroRW
}
object ResultSaved {
  implicit val rw: RW[ResultSaved] = macroRW
}
