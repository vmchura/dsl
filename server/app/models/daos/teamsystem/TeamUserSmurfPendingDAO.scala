package models.daos.teamsystem

import models.teamsystem.{PendingSmurf, TeamID}
import shared.models.DiscordID

import scala.concurrent.Future

trait TeamUserSmurfPendingDAO {
  def add(
      pendingSmurf: PendingSmurf
  ): Future[Boolean]
  def load(discordID: DiscordID): Future[Seq[PendingSmurf]]
  def loadFromTeam(teamID: TeamID): Future[Seq[PendingSmurf]]
}
