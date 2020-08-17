package models.services

import java.util.UUID

import models.UserSmurf

import scala.concurrent.Future

trait SmurfService {
  def acceptSmurf(discordUserID: String, matchID: UUID): Future[Boolean]
  def declineSmurf(discordUserID: String, matchID: UUID): Future[Boolean]
  def showAcceptedSmurfs(): Future[Seq[UserSmurf]]
}
