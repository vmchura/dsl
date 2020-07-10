package models.services

import models.ChallongeTournament

import scala.concurrent.Future

trait ChallongeTournamentService {
  protected def challongeApiKey: String
  def findChallongeTournament(discordServerID: String)(tournamentID: String): Future[Option[ChallongeTournament]]
}
