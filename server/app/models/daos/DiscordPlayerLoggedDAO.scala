package models.daos

import models.DiscordPlayerLogged
import shared.models.DiscordID

import scala.concurrent.Future

trait DiscordPlayerLoggedDAO {
  def add(discordPlayerLogged: DiscordPlayerLogged): Future[Boolean]
  def load(discordID: DiscordID): Future[Option[DiscordPlayerLogged]]
  def find(query: String): Future[List[DiscordPlayerLogged]]
}
