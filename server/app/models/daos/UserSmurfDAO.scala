package models.daos



import models.{DiscordUser, MatchSmurf, UserSmurf}

import scala.concurrent.Future

trait UserSmurfDAO {
  def addUser(discordUser: DiscordUser): Future[Boolean]
  def findUser(discordUserID: String): Future[Option[UserSmurf]]
  def findUsers(discordUsersID: Seq[String]): Future[Seq[UserSmurf]]
  def findBySmurf(smurf: String): Future[List[UserSmurf]]
  def addSmurf(discordUserID: String, matchSmurf: MatchSmurf): Future[Boolean]
  def addNotCheckedSmurf(discordUserID: String, matchSmurf: MatchSmurf): Future[Boolean]
  def removeSmurf(discordUserID : String, smurfToRemove: MatchSmurf): Future[Boolean]
  def acceptNotCheckedSmurf(discordUserID : String, smurfToRemove: MatchSmurf): Future[Boolean]
  def removeUser(discordUserID: String): Future[Boolean]
  def findUsersNotCompletelyDefined(): Future[Seq[UserSmurf]]
}
