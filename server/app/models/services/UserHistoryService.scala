package models.services

import models.{DiscordID, DiscordUserData, GuildID}

import scala.concurrent.Future

trait UserHistoryService {
  def update(): Future[Int]
  def update(discordID: DiscordID,guildID: GuildID, guilds: Map[GuildID,Seq[DiscordUserData]]): Future[Int]
}
