package models.teamsystem

import shared.models.{DiscordID, ReplayTeamID}
import play.api.libs.json._

import java.util.Date
case class Points(
    teamID: TeamID,
    replayTeamID: ReplayTeamID,
    points: Int,
    userDiscordID: DiscordID,
    date: Date,
    reason: String,
    enabled: Boolean
)
object Points {
  import models.ModelsJsonImplicits._
  implicit val pointsJsonFormat: OFormat[Points] =
    Json.format[Points]
}
