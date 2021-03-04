package models.daos.teamsystem

import shared.models.{ReplayTeamID, ReplayTeamRecord}

import scala.concurrent.Future

trait TeamMetaReplayTeamDAO {
  def save(replay: ReplayTeamRecord): Future[Boolean]
  def isRegistered(hash: String): Future[Boolean]
  def load(replayTeamID: ReplayTeamID): Future[Option[ReplayTeamRecord]]
}
