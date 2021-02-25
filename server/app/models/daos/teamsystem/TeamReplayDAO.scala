package models.daos.teamsystem

import models.teamsystem.TeamReplayInfo

import scala.concurrent.Future

trait TeamReplayDAO {
  def save(replayInfo: TeamReplayInfo): Future[TeamReplayInfo]
}
