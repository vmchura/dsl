package models.services

import models.{DiscordPlayerLogged, DiscordUser, DiscordUserData, GuildID}
import shared.models.DiscordID

import scala.concurrent.Future

trait DiscordUserService {
  protected def bot_token: String
  def findMembersOnGuild(guildID: String): Future[Option[Seq[DiscordUser]]]
  def findMembersOnGuildData(
      guildID: GuildID
  ): Future[Option[Seq[DiscordUserData]]]
  def findMember(discordID: DiscordID): Future[Option[DiscordPlayerLogged]]
}
