package models.services

import models.ChallongeTournament

import scala.concurrent.Future

trait ChallongeUserService {
  def findChallongeTournament(challongeApiKey: String)(discordServerID: String)(tournamentID: String): Future[Option[ChallongeTournament]]
}
