package models.services

import models.{DiscordID, DiscordUser, DiscordUserData, GuildID}

import scala.concurrent.Future

trait DiscordUserService {
  protected def bot_token: String
  def findMembersOnGuild(guildID: String): Future[Option[Seq[DiscordUser]]]
  def loadSingleUser(guildID: GuildID, discordID: DiscordID): Future[Option[DiscordUserData]]
}
