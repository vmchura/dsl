package models.daos.teamsystem

import models.teamsystem.{PendingSmurf, TeamID}
import shared.models.{DiscordID, ReplayTeamID}

import scala.concurrent.Future

trait TeamUserSmurfPendingDAO {
  def add(
      pendingSmurf: PendingSmurf
  ): Future[Boolean]
  def load(discordID: DiscordID): Future[Seq[PendingSmurf]]
  def load(replayTeamID: ReplayTeamID): Future[Option[PendingSmurf]]
  def loadRelated(replayTeamID: ReplayTeamID): Future[Seq[PendingSmurf]]
  def remove(replayTeamID: ReplayTeamID): Future[Boolean]
  def removeRelated(replayTeamID: ReplayTeamID): Future[Boolean]
  def loadFromTeam(teamID: TeamID): Future[Seq[PendingSmurf]]
}
