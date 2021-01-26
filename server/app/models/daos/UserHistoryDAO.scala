package models.daos

import models.{DiscordUserData, DiscordUserHistory, GuildID}
import shared.models.DiscordID

import scala.concurrent.Future

trait UserHistoryDAO {
  def all(): Future[Seq[DiscordUserHistory]]
  def load(discordID: DiscordID): Future[Option[DiscordUserHistory]]
  def updateWithLastInformation(
      discordID: DiscordID,
      guildID: GuildID,
      data: DiscordUserData
  ): Future[Boolean]
  protected def register(
      discordID: DiscordID,
      guildID: GuildID,
      data: DiscordUserData
  ): Future[Boolean]
  protected def updateLastUserName(
      discordID: DiscordID,
      guildID: GuildID,
      data: DiscordUserData
  ): Future[Boolean]
}
