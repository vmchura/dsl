package models.services

import java.util.UUID

import models.{Participant, ParticipantDefined, ParticipantPK}

import scala.concurrent.Future

trait ParticipantsService {
  def saveParticipant(participant: Participant): Future[Boolean]
  def loadParticipant(participantPK: ParticipantPK): Future[Option[Participant]]
  def loadParticipantsWithNoRelation(challongeID: Long): Future[Seq[Participant]]
  def updateParticipantRelation(participant: Participant): Future[Boolean]
  def loadParticipantByUserID(userID: UUID): Future[Seq[Participant]]
  def loadParticipantByDiscordUserID(discordUserID: String): Future[Seq[Participant]]
  def loadParticipantDefinedByTournamentID(challongeID: Long): Future[Seq[ParticipantDefined]]
  def dropParticipant(participantPK: ParticipantPK): Future[Boolean]

}
