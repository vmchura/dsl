package models.services
import java.util.UUID

import javax.inject.Inject
import models.daos.ParticipantDAO
import models.{Participant, ParticipantDefined, ParticipantPK}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ParticipantsServiceImpl @Inject() (participantDAO: ParticipantDAO) extends ParticipantsService {
  override def saveParticipant(participant: Participant): Future[Boolean] = participantDAO.save(participant)

  override def loadParticipant(participantPK: ParticipantPK): Future[Option[Participant]] = participantDAO.find(participantPK)

  override def loadParticipantsWithNoRelation(challongeID: Long): Future[Seq[Participant]] = participantDAO.findByTournamentID(challongeID).map(_.filter(_.discordUserID.isEmpty))

  override def updateParticipantRelation(participant: Participant): Future[Boolean] = participantDAO.save(participant)

  override def loadParticipantByUserID(userID: UUID): Future[Seq[Participant]] = participantDAO.findByUserID(userID)

  override def dropParticipant(participantPK: ParticipantPK): Future[Boolean] = participantDAO.drop(participantPK)

  override def loadParticipantByDiscordUserID(discordUserID: String): Future[Seq[Participant]] = participantDAO.findByDiscordUserID(discordUserID)

  override def loadParticipantDefinedByTournamentID(challongeID: Long): Future[Seq[ParticipantDefined]] = participantDAO.findDefinedByTournamentID(challongeID)

}
