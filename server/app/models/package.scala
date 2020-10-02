import play.api.libs.json.{Json, OFormat}

package object models {

  case class GuildID(id: String) extends AnyVal
  object GuildID{
    implicit val jsonFormat: OFormat[GuildID] = Json.format[GuildID]
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
