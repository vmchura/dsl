package models.daos

import models.{GuildID, UserGuild}
import shared.models.DiscordID

import scala.concurrent.Future

trait UserGuildDAO {
  def load(discordID: DiscordID): Future[Set[GuildID]]
  def addGuildToUser(discordID: DiscordID, guildID: GuildID): Future[Boolean]
  def all(): Future[Seq[UserGuild]]
  def guilds(): Future[Seq[GuildID]]
}
