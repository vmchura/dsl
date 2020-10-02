package models.daos

import models.{DiscordID, GuildID}

import scala.concurrent.Future

trait UserGuildDAO {
  def load(discordID: DiscordID): Future[Set[GuildID]]
  def addGuildToUser(discordID: DiscordID, guildID: GuildID): Future[Boolean]
}
