package models.daos

import java.util.UUID

import models.Tournament

import scala.concurrent.Future

trait TournamentDAO {
  def all(): Future[Seq[Tournament]]
  def save(tournament: Tournament): Future[Boolean]
  def load(tournamentID: UUID): Future[Option[Tournament]]
}
