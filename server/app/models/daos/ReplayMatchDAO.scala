package models.daos
import java.util.UUID

import models.ReplayRecord

import scala.concurrent.Future
trait ReplayMatchDAO {

  def add(replayRecord: ReplayRecord): Future[Boolean]
  def markAsDisabled(replayID: UUID): Future[Boolean]
  def loadAllByTournament(tournamentID: Long): Future[Seq[ReplayRecord]]
  def loadAllByMatch(tournamentID: Long, matchID: Long): Future[Seq[ReplayRecord]]
}
