package shared.models

case class DiscordPlayerLogged(
    discordID: DiscordID,
    username: String,
    discriminator: DiscordDiscriminator,
    avatar: Option[String]
)
