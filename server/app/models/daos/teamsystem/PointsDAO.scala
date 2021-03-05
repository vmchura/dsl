package models.daos.teamsystem

import models.teamsystem.{Points, TeamID}
import shared.models.ReplayTeamID

import java.util.Date
import scala.concurrent.Future

trait PointsDAO {
  def save(points: Points): Future[Boolean]
  def load(replayTeamID: ReplayTeamID): Future[Seq[Points]]
  def load(teamID: TeamID, enabled: Boolean = true): Future[Seq[Points]]
  def disableByTime(threadsHold: Date): Future[Int]
}
