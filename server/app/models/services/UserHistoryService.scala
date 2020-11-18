package models.services

import models.{DiscordID, DiscordUserData, GuildID}

import scala.concurrent.Future

trait UserHistoryService {
  def update(): Future[Int]
  def update(
      discordUserData: DiscordUserData,
      guildID: models.GuildID
  ): Future[Int]
}
