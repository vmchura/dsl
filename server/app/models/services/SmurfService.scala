package models.services

import java.util.UUID

import scala.concurrent.Future

trait SmurfService {
  def acceptSmurf(discordUserID: String, matchID: UUID): Future[Boolean]
  def declineSmurf(discordUserID: String, matchID: UUID): Future[Boolean]
}
