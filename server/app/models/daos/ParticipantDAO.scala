package models.daos

import java.util.UUID

import models.{Participant, ParticipantDefined, ParticipantPK}

import scala.concurrent.Future

trait ParticipantDAO {
  def find(participantPK: ParticipantPK): Future[Option[Participant]]
  def save(participant: Participant): Future[Boolean]
  def findByTournamentID(challongeID: Long): Future[Seq[Participant]]
  def findByUserID(userID: UUID): Future[Seq[Participant]]
  def findByDiscordUserID(discordUserID: String): Future[Seq[Participant]]
  def findDefinedByTournamentID(challongeID: Long): Future[Seq[ParticipantDefined]]
  def drop(participantPK: ParticipantPK): Future[Boolean]
  def findBySmurf(smurf: String): Future[Seq[ParticipantDefined]]
  def addSmurf(participantPK: ParticipantPK, newSmurf: String): Future[Boolean]
  def removeSmurf(participantPK: ParticipantPK, smurfToRemove: String): Future[Boolean]

}
