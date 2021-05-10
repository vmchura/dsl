package models.daos

import models.{AuthToken, TrovoUser, TrovoUserID}

import java.util.UUID
import scala.concurrent.Future

/**
  * Give access to the [[AuthToken]] object.
  */
trait TrovoUserDAO {

  def all(): Future[List[TrovoUser]]
  def find(discordID: String): Future[Option[TrovoUser]]
  def find(trovoUserID: TrovoUserID): Future[Option[TrovoUser]]

  def save(trovoUser: TrovoUser): Future[Option[TrovoUser]]
  def update(trovoUser: TrovoUser): Future[Option[TrovoUser]]

  def remove(discordID: String): Future[Boolean]
}
