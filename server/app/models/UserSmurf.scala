package models

import play.api.libs.json.{Json, OFormat}

case class MatchSmurf(matchPK: MatchPK, smurf: String)
object MatchSmurf{
  implicit val jsonFormat: OFormat[MatchSmurf] = Json.format[MatchSmurf]

}


case class UserSmurf(discordUser: DiscordUser, matchSmurf: Seq[MatchSmurf])

object UserSmurf {
  implicit val jsonFormat: OFormat[UserSmurf] = Json.format[UserSmurf]
}

