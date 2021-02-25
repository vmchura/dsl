package models.teamsystem

import play.api.libs.json.{Json, OFormat}
import shared.models.{DiscordID, ReplayTeamID}

case class TeamReplayInfo(
    replayID: ReplayTeamID,
    cloudLocation: String,
    senderID: DiscordID
)
object TeamReplayInfo {
  import models.ModelsJsonImplicits._
  implicit val jsonFormat: OFormat[TeamReplayInfo] = Json.format[TeamReplayInfo]

}
