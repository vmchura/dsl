package models
import play.api.libs.json._
import shared.utils.ComparableByLabel

case class DiscordUser(discordID: String, userName: String) extends ComparableByLabel {
  override def stringLabel: String = userName
}

object DiscordUser {
  implicit val jsonFormat: OFormat[DiscordUser] = Json.format[DiscordUser]
}