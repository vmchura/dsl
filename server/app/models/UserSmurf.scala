package models

import java.util.UUID

import play.api.libs.json.{Json, OFormat}

case class MatchSmurf(resultID: UUID,matchPK: MatchPK, smurf: String)
object MatchSmurf{
  implicit val jsonFormat: OFormat[MatchSmurf] = Json.format[MatchSmurf]

}


case class UserSmurf(discordUser: DiscordUser, matchSmurf: Seq[MatchSmurf], notCheckedSmurf: Seq[MatchSmurf])

object UserSmurf {
  implicit val jsonFormat: OFormat[UserSmurf] = Json.format[UserSmurf]
}


