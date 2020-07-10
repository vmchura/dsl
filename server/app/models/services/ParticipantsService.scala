package models.services

import java.util.UUID

import models.{Participant, ParticipantPK}

import scala.concurrent.Future

trait ParticipantsService {
  def saveParticipant(participant: Participant): Future[Boolean]
  def loadParticipant(participantPK: ParticipantPK): Future[Option[Participant]]
  def loadParticipantsWithNoRelation(tournamentID: UUID): Future[Seq[Participant]]
  def updateParticipantRelation(participant: Participant): Future[Boolean]
  def loadParticipantByUserID(userID: UUID): Future[Seq[Participant]]
  def dropParticipant(participantPK: ParticipantPK): Future[Boolean]
}
