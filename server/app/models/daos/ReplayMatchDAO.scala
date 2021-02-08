package models.daos
import java.io.File
import java.util.UUID

import models.ReplayRecord

import scala.concurrent.Future
trait ReplayMatchDAO {

  def add(replayRecord: ReplayRecord): Future[Boolean]
  def markAsDisabled(replayID: UUID): Future[Boolean]
  def loadAllByTournament(tournamentID: Long): Future[Seq[ReplayRecord]]
  def loadAllByMatch(
      tournamentID: Long,
      matchID: Long
  ): Future[Seq[ReplayRecord]]
  def find(replayID: UUID): Future[Option[ReplayRecord]]
  def isNotRegistered(file: File): Future[Boolean]
  def isRegistered(hash: String): Future[Boolean]
  def updateLocation(replayID: UUID, cloudLocation: String): Future[Boolean]
}
