package models.daos.teamsystem

import models.teamsystem.TeamReplayInfo
import shared.models.ReplayTeamID

import scala.concurrent.Future

trait TeamReplayDAO {
  def save(replayInfo: TeamReplayInfo): Future[TeamReplayInfo]
  def load(replayTeamID: ReplayTeamID): Future[Option[TeamReplayInfo]]
}
