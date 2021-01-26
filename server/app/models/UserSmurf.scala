package models

import java.util.UUID
import play.api.libs.json.{Json, OFormat}
import shared.models.DiscordID

case class MatchSmurf(resultID: UUID, matchPK: MatchPK, smurf: String)
object MatchSmurf {
  implicit val jsonFormat: OFormat[MatchSmurf] = Json.format[MatchSmurf]

}

case class UserSmurf(
    discordUser: DiscordUser,
    matchSmurf: Seq[MatchSmurf],
    notCheckedSmurf: Seq[MatchSmurf]
)

object UserSmurf {
  implicit val jsonFormat: OFormat[UserSmurf] = Json.format[UserSmurf]
}

case class ValidUserSmurf(discordID: DiscordID, smurfs: Seq[Smurf])
object ValidUserSmurf {
  import ModelsJsonImplicits._
  implicit val jsonFormat: OFormat[ValidUserSmurf] = Json.format[ValidUserSmurf]
}
