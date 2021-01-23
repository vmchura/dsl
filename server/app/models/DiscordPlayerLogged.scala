package models

import play.api.libs.json.{Json, OFormat}
import be.venneborg.refined.play.RefinedJsonFormats._
case class DiscordPlayerLogged(
    discordID: DiscordID,
    username: String,
    discriminator: DiscordDiscriminator,
    avatar: Option[String]
)
object DiscordPlayerLogged {
  implicit val jsonFormat: OFormat[DiscordPlayerLogged] =
    Json.format[DiscordPlayerLogged]
}
