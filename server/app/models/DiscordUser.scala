package models
import play.api.libs.json._

case class DiscordUser(discordID: String, userName: String)

object DiscordUser {
  implicit val jsonFormat: OFormat[DiscordUser] = Json.format[DiscordUser]
}