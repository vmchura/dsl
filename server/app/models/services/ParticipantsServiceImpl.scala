package models.services
import java.util.UUID

import javax.inject.Inject
import models.daos.ParticipantDAO
import models.{Participant, ParticipantPK}
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future

class ParticipantsServiceImpl @Inject() (participantDAO: ParticipantDAO) extends ParticipantsService {
  override def saveParticipant(participant: Participant): Future[Boolean] = participantDAO.save(participant)

  override def loadParticipant(participantPK: ParticipantPK): Future[Option[Participant]] = participantDAO.find(participantPK)

  override def loadParticipantsWithNoRelation(tournamentID: UUID): Future[Seq[Participant]] = participantDAO.find(tournamentID).map(_.filter(_.discordUserID.isEmpty))

  override def updateParticipantRelation(participant: Participant): Future[Boolean] = participantDAO.save(participant)
}
