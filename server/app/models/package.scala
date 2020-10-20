import java.util.UUID

import eu.timepit.refined.api.Refined
import eu.timepit.refined.W
import eu.timepit.refined.string._
import play.api.libs.json.{Json, OFormat}

package object models {

  type DiscordDiscriminator = String Refined MatchesRegex[W.`"""[0-9]{4}"""`.T]

  case class GuildID(id: String) extends AnyVal
  object GuildID{
    implicit val jsonFormat: OFormat[GuildID] = Json.format[GuildID]
  }

  case class UserHistoryID(id: UUID) extends AnyVal
  object UserHistoryID{
    implicit val jsonFormat: OFormat[UserHistoryID] = Json.format[UserHistoryID]
  }

  case class DiscordID(id: String) extends AnyVal
  object DiscordID{
    implicit val jsonFormat: OFormat[DiscordID] = Json.format[DiscordID]
  }
  case class Smurf(name: String) extends AnyVal

  object Smurf{
    implicit val jsonFormat: OFormat[Smurf] = Json.format[Smurf]
  }
}
