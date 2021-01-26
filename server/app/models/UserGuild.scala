package models
import play.api.libs.json.{Json, OFormat}
import shared.models.DiscordID
case class UserGuild(discordID: DiscordID, guilds: Set[GuildID])
object UserGuild {
  import ModelsJsonImplicits._
  implicit val jsonFormat: OFormat[UserGuild] = Json.format[UserGuild]
}
