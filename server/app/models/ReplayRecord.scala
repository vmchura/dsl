package models

import java.util.UUID

import play.api.libs.json.{Json, OFormat}
import shared.models.ReplayRecordShared

case class ReplayRecord(replayID: UUID,
                        matchName: String, nombreOriginal: String,
                        tournamentID: Long, matchID: Long, enabled: Boolean,
                        uploaderDiscordID: String){
  def sharedVersion(): ReplayRecordShared = ReplayRecordShared(replayID,matchName,nombreOriginal,enabled)
}
object ReplayRecord{
  implicit val jsonFormat: OFormat[ReplayRecord] = Json.format[ReplayRecord]

}
