package models.services

import models.ChallongeTournament

import scala.concurrent.Future

trait ChallongeTournamentService {
  protected def challongeApiKey: String
  def findChallongeTournament(discordServerID: String,discordChanelReplayID: Option[String] = None)(tournamentID: String): Future[Option[ChallongeTournament]]
}
object ChallongeTournamentService {
  def buildUri(urlID: String): String = s"https://challonge.com/es/$urlID"
}
