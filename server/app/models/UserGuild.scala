package models
import play.api.libs.json.{Json, OFormat}
case class UserGuild(discordID: DiscordID, guilds: Set[GuildID])
object UserGuild{
  implicit val jsonFormat: OFormat[UserGuild] = Json.format[UserGuild]
}
