package models

import play.api.libs.json.{Json, OFormat}
import be.venneborg.refined.play.RefinedJsonFormats._
import shared.models.DiscordID
case class DiscordPlayerLogged(
    discordID: DiscordID,
    username: String,
    discriminator: DiscordDiscriminator,
    avatar: Option[String]
)
object DiscordPlayerLogged {
  import ModelsJsonImplicits._
  implicit val jsonFormat: OFormat[DiscordPlayerLogged] =
    Json.format[DiscordPlayerLogged]
}
