package models.services

import models.DiscordUser

import scala.concurrent.Future

trait DiscordUserService {
//https://discord.com/api/guilds/728442814832312372/members
  //header Authorization
  // Bot BOt_token
  def findMembersOnGuild(bot_token: String)(guildID: String): Future[Seq[DiscordUser]]
}
