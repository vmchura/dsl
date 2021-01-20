package models.daos.teamsystem

import models.DiscordID
import models.teamsystem.{Team, TeamID}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
trait TeamDAO {
  def save(userID: DiscordID, teamName: String): Future[TeamID]
  def loadTeams(): Future[Seq[Team]]
  def isOfficial(userID: DiscordID): Future[Boolean] =
    loadTeams().map(_.exists(_.isOfficial(userID)))
  def canBeAddedAsSuplente(userID: DiscordID, teamID: TeamID): Future[Boolean] =
    loadTeams().map(
      _.find(_.teamID == teamID).fold(false)(_.canBeAddedAsSuplente(userID))
    )
  def isOfficial(userID: DiscordID, teamID: TeamID): Future[Boolean] =
    loadTeams().map(
      _.find(_.teamID == teamID).fold(false)(_.isOfficial(userID))
    )
  def isMember(userID: DiscordID, teamID: TeamID): Future[Boolean] =
    loadTeams().map(
      _.find(_.teamID == teamID).fold(false)(_.isMember(userID))
    )
  def isPrincipal(userID: DiscordID, teamID: TeamID): Future[Boolean] =
    loadTeams().map(
      _.find(_.teamID == teamID).fold(false)(_.principal == userID)
    )
}
