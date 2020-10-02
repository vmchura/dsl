package models.services

import models.{DiscordID, GuildID}

import scala.concurrent.Future

trait UserHistoryService {
  def update(): Future[Int]
  def update(discordID: DiscordID,guildID: GuildID): Future[Int]
}
