package models

import play.api.libs.json.{Json, OFormat}
import shared.models.DiscordID

case class UserLeftGuild(discordID: DiscordID, guild: GuildID)
object UserLeftGuild {
  import ModelsJsonImplicits._
  implicit val jsonFormat: OFormat[UserLeftGuild] = Json.format[UserLeftGuild]
}
