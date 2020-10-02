package models.daos

import models.{DiscordID, GuildID, UserGuild}

import scala.concurrent.Future

trait UserGuildDAO {
  def load(discordID: DiscordID): Future[Set[GuildID]]
  def addGuildToUser(discordID: DiscordID, guildID: GuildID): Future[Boolean]
  def all(): Future[Seq[UserGuild]]
}
