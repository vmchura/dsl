package models.daos

import models.{DiscordDiscriminator, DiscordID, DiscordUserData, DiscordUserHistory, GuildID}

import scala.concurrent.Future

trait UserHistoryDAO {
  def load(discordID: DiscordID): Future[Option[DiscordUserHistory]]
  def updateWithLastInformation(discordID: models.DiscordID, guildID: GuildID, data: DiscordUserData): Future[Boolean]
  protected def register(discordID: DiscordID, guildID: GuildID, data: DiscordUserData): Future[Boolean]
  protected def updateLastUserName(discordID: DiscordID, guildID: GuildID, data: DiscordUserData): Future[Boolean]
}
