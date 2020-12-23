package models.services.usertrajectory

import models.ReplayRecordResumen
import shared.models.ChallongeOneVsOneDefined

import scala.concurrent.Future

/**
  * It provides basic information about an user
  */
trait UserTrajectoryService {
  def loadReplays(discordID: String): Future[Seq[ChallongeOneVsOneDefined]]
  def pushHistoryReplay(replayRecordResumen: ReplayRecordResumen): Future[Unit]
}
