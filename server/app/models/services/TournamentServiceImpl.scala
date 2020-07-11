package models.services
import java.util.UUID

import javax.inject.Inject
import models.daos.{ParticipantDAO, TournamentDAO}
import models.Tournament

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
class TournamentServiceImpl @Inject() (tournamentDAO: TournamentDAO, participantDAO: ParticipantDAO) extends TournamentService {
  override def findAllTournaments(): Future[Seq[Tournament]] =  tournamentDAO.all()

  override def findAllActiveTournaments(): Future[Seq[Tournament]] = tournamentDAO.all().map(_.filter(_.active))

  override def saveTournament(tournament: Tournament): Future[Boolean] = tournamentDAO.save(tournament)

  override def loadTournament(challongeID: Long): Future[Option[Tournament]] = tournamentDAO.load(challongeID)

  private def findTournamentByPlayer(userID: UUID, tournamentQuery: Long => Future[Option[Tournament]]) = {
    for {
      participants    <- participantDAO.findByUserID(userID)
      tournamentsID   <- Future.successful(participants.map(_.participantPK.challongeID).distinct)
      tournaments     <- Future.sequence(tournamentsID.map(tournamentQuery))
    }yield{
      tournaments.flatten
    }
  }

  override def findAllTournamentsByPlayer(userID: UUID): Future[Seq[Tournament]] = findTournamentByPlayer(userID,loadTournament)

  private def findTournamentIfActive(challongeID: Long) = loadTournament(challongeID).map(_.flatMap(t => if(t.active) Some(t) else None))
  override def findAllActiveTournamentsByPlayer(userID: UUID): Future[Seq[Tournament]] = findTournamentByPlayer(userID,findTournamentIfActive)

  override def dropTournament(challongeID: Long): Future[Boolean] = tournamentDAO.remove(challongeID)
}
