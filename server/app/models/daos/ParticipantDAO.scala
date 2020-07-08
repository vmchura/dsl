package models.daos

import java.util.UUID

import models.{Participant, ParticipantPK}

import scala.concurrent.Future

trait ParticipantDAO {
  def find(participantPK: ParticipantPK): Future[Option[Participant]]
  def save(participant: Participant): Future[Boolean]
  def find(tournamentID: UUID): Future[Seq[Participant]]
}
