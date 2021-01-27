package models.daos.teamsystem

import models.teamsystem.{Member, Team, TeamID}
import shared.models.DiscordID

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
trait TeamDAO {
  def removeMember(userID: DiscordID, teamID: TeamID): Future[Boolean]

  def removeTeam(teamID: TeamID): Future[Boolean]
  def loadTeam(teamID: TeamID): Future[Option[Team]]
  def save(userID: DiscordID, teamName: String): Future[TeamID]
  def loadTeams(): Future[Seq[Team]]
  def addMemberTo(member: Member, teamID: TeamID): Future[Boolean]
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
  def teamsOf(userID: DiscordID): Future[Seq[Team]] =
    loadTeams().map(_.filter(_.isMember(userID)))
}
