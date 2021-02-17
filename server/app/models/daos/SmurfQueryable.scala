package models.daos

import models.{Smurf, ValidUserSmurf}
import shared.models.DiscordID

import scala.concurrent.Future

trait SmurfQueryable {
  def findOwner(smurf: Smurf): Future[Option[DiscordID]]
  def add(discordID: DiscordID, smurf: Smurf): Future[Boolean]
  def load(discordID: DiscordID): Future[Option[ValidUserSmurf]]
  def all(): Future[Seq[ValidUserSmurf]]
}
