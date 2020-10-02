package models.daos

import models.{DiscordDiscriminator, DiscordID, DiscordUserHistory, DiscordUserLog}

import scala.concurrent.Future

trait UserHistoryDAO {
  def load(discordID: DiscordID): Future[Option[DiscordUserHistory]]
  def updateWithLastInformation(discordID: models.DiscordID, discriminator: DiscordDiscriminator,userLog: String): Future[Boolean]
  protected def register(discordID: DiscordID, discriminator: DiscordDiscriminator, userLog: String): Future[Boolean]
  protected def updateLastUserName(discordID: DiscordID, userLog: String): Future[Boolean]
}
