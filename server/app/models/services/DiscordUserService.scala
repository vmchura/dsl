package models.services

import shared.DiscordUser

import scala.concurrent.Future

trait DiscordUserService {
//https://discord.com/api/guilds/728442814832312372/members
  //header Authorization
  // Bot BOt_token

  def BOT_TOKEN: String

  def findMembersOnGuild(guildID: String): Future[Seq[DiscordUser]]
}
