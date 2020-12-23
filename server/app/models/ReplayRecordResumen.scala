package models

import play.api.libs.json.{JsObject, JsResult, JsString, JsValue, Json, OFormat}
import shared.models.StarCraftModels.{SCPlayer, SCRace}

import java.util.UUID
import scala.util.Try
case class DiscordPlayer(discordID: Option[String], gamePlayer: SCPlayer)
object DiscordPlayer {
  implicit val scRace: OFormat[SCRace] = new OFormat[SCRace] {
    override def writes(o: SCRace): JsObject =
      JsObject(Map("race" -> JsString(o.str)))

    override def reads(json: JsValue): JsResult[SCRace] =
      JsResult.fromTry(Try { SCRace((json \ "race").as[String]) })
  }
  implicit val scPlayer: OFormat[SCPlayer] =
    Json.format[SCPlayer]
  implicit val discordPlayer: OFormat[DiscordPlayer] =
    Json.format[DiscordPlayer]
}
case class ReplayRecordResumen(
    replayID: UUID,
    winner: DiscordPlayer,
    loser: DiscordPlayer,
    enabled: Boolean
)
object ReplayRecordResumen {

  implicit val jsonFormat: OFormat[ReplayRecordResumen] =
    Json.format[ReplayRecordResumen]
}
