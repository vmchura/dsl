package models.services

import java.util.UUID

import models.Tournament

import scala.concurrent.Future

trait TournamentService {
  def findAllTournaments(): Future[Seq[Tournament]]
  def findAllActiveTournaments(): Future[Seq[Tournament]]
  def saveTournament(tournament: Tournament): Future[Boolean]
  def loadTournament(tournamentID: UUID): Future[Option[Tournament]]
  def findAllTournamentsByPlayer(userID: UUID): Future[Seq[Tournament]]
  def findAllActiveTournamentsByPlayer(userID: UUID): Future[Seq[Tournament]]
  def dropTournament(tournamentID: UUID): Future[Boolean]
}
