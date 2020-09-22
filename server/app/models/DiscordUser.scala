package models
import play.api.libs.json._
import shared.utils.ComparableByLabel

case class DiscordUser(discordID: String, userName: String, discriminator: Option[String]) extends ComparableByLabel {
  override def stringLabel: String = s"$userName"
}

object DiscordUser {
  implicit val jsonFormat: OFormat[DiscordUser] = Json.format[DiscordUser]
}