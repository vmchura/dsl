package models.services

import java.util.UUID

import models.{DiscordID, Smurf, UserSmurf, ValidUserSmurf}

import scala.concurrent.Future

trait SmurfService {
  import SmurfService.SmurfAdditionResult.AdditionResult
  def acceptSmurf(discordUserID: String, matchID: UUID): Future[Boolean]
  def declineSmurf(discordUserID: String, matchID: UUID): Future[Boolean]
  def showAcceptedSmurfs(): Future[Seq[UserSmurf]]
  def addSmurf(discordID: DiscordID, smurf: Smurf): Future[AdditionResult]
  def loadValidSmurfs(): Future[Seq[ValidUserSmurf]]
  def loadSmurfs(discordID: DiscordID): Future[Option[ValidUserSmurf]]
  def findOwner(smurf: Smurf): Future[Option[DiscordID]]
}
object SmurfService {
  object SmurfAdditionResult extends Enumeration {
    type AdditionResult = Value
    val Added, AlreadyRegistered, CantBeAdded = Value
  }
}
