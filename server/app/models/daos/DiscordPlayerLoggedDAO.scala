package models.daos

import models.{DiscordID, DiscordPlayerLogged}

import scala.concurrent.Future

trait DiscordPlayerLoggedDAO {
  def add(discordPlayerLogged: DiscordPlayerLogged): Future[Boolean]
  def load(discordID: DiscordID): Future[Option[DiscordPlayerLogged]]
  def find(query: String): Future[List[DiscordPlayerLogged]]
}
