package models.daos.teamsystem

import models.Smurf
import models.teamsystem.TeamID
import shared.models.{DiscordID, ReplayTeamID}

import scala.concurrent.Future

trait TeamUserSmurfPendingDAO {
  def add(
      discordID: DiscordID,
      smurf: Smurf,
      replayTeamID: ReplayTeamID
  ): Future[Boolean]
  def load(discordID: DiscordID): Future[Seq[(Smurf, ReplayTeamID)]]
  def loadFromTeam(teamID: TeamID): Future[Seq[(Smurf, ReplayTeamID)]]
}
