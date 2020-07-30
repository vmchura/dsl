package models

import java.util.UUID

import play.api.libs.json.{Json, OFormat}

case class MatchResult(matchResultID: UUID,
                       tournamentID: Long, matchID: Long,
                       firstDiscordPlayer: String, secondDiscordPlayer: String,
                       firstPlayerOnGame: String, secondPlayerOnGame: String,
                       winner: Int, uploadedOnChallonge: Boolean, nicksSameValue: Boolean)
object MatchResult{
  implicit val jsonFormat: OFormat[MatchResult] = Json.format[MatchResult]

}
