import play.api.libs.json.{Json, OFormat}

package object models {
  case class DiscordID(id: String) extends AnyVal{


  }
  object DiscordID{
    implicit val jsonFormat: OFormat[DiscordID] = Json.format[DiscordID]
  }
  case class Smurf(name: String) extends AnyVal

  object Smurf{
    implicit val jsonFormat: OFormat[Smurf] = Json.format[Smurf]
  }
}
