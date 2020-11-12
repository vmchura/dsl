package models.services

import models.{Tournament, TournamentSerieID, TournamentSeries}

import scala.concurrent.Future

trait TournamentSeriesService {
  def createSeries(series: TournamentSeries): Future[TournamentSeries]
  def addSeason(
      id: TournamentSerieID,
      tournament: Tournament,
      season: Int
  ): Future[Boolean]
  def allSeries(): Future[Seq[TournamentSeries]]
  def findSeries(id: TournamentSerieID): Future[Option[TournamentSeries]]
}
