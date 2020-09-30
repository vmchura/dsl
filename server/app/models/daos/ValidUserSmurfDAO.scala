package models.daos

import models.{DiscordID, Smurf, ValidUserSmurf}

import scala.concurrent.Future

trait ValidUserSmurfDAO {
  def add(discordID: DiscordID, smurf: Smurf): Future[Boolean]
  def load(discordID: DiscordID): Future[Option[ValidUserSmurf]]
  def findOwner(smurf: Smurf): Future[Option[DiscordID]]
  def all(): Future[Seq[ValidUserSmurf]]
}
