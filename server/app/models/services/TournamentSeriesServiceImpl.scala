package models.services
import models.{
  Tournament,
  TournamentSeason,
  TournamentSerieID,
  TournamentSeries
}

import javax.inject._
import play.api.libs.json._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import reactivemongo.api._
import reactivemongo.play.json._
import collection._
import play.modules.reactivemongo._
import reactivemongo.play.json.collection.JSONCollection

class TournamentSeriesServiceImpl @Inject() (
    val reactiveMongoApi: ReactiveMongoApi
) extends TournamentSeriesService {
  def collection: Future[JSONCollection] =
    reactiveMongoApi.database.map(_.collection("dsl.tournamentseries"))

  override def createSeries(
      series: TournamentSeries
  ): Future[TournamentSeries] =
    collection.flatMap(_.insert(ordered = true).one(series)).map(_ => series)

  override def addSeason(
      id: TournamentSerieID,
      tournament: Tournament,
      season: Int,
      winners: List[(Int, String)]
  ): Future[Boolean] = {
    collection
      .flatMap(
        _.update(ordered = true).one(
          Json.obj("id" -> id),
          Json.obj(
            "$push" -> Json.obj(
              "seasons" -> TournamentSeason(
                tournament.challongeID,
                season,
                winners
              )
            )
          ),
          upsert = true
        )
      )
      .map(_.ok)
  }

  override def allSeries(): Future[Seq[TournamentSeries]] =
    collection.flatMap(
      _.find(Json.obj(), Option.empty[TournamentSeries])
        .cursor[TournamentSeries]()
        .collect[List](-1, Cursor.FailOnError[List[TournamentSeries]]())
    )

  override def findSeries(
      id: TournamentSerieID
  ): Future[Option[TournamentSeries]] =
    collection
      .flatMap(
        _.find(Json.obj("id" -> id), Option.empty[TournamentSeries])
          .one[TournamentSeries]
      )
}
