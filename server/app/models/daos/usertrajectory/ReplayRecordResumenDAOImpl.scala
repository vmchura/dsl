package models.daos.usertrajectory
import models.ReplayRecordResumen

import javax.inject._
import play.api.libs.json._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import reactivemongo.api._
import reactivemongo.play.json.compat._
import play.modules.reactivemongo._
import reactivemongo.play.json.collection.JSONCollection
import ReplayRecordResumenDAO._

class ReplayRecordResumenDAOImpl @Inject() (
    val reactiveMongoApi: ReactiveMongoApi
) extends ReplayRecordResumenDAO {

  def collection: Future[JSONCollection] =
    reactiveMongoApi.database.map(
      _.collection("racesurvivor.replayrecordresumen")
    )

  override def add(
      replayRecordResumen: ReplayRecordResumen
  ): Future[ReplayRecordResumen] = {
    collection
      .flatMap(_.insert(ordered = true).one(replayRecordResumen))
      .map(_ => replayRecordResumen)
  }

  override def load(
      param: SearchParam
  ): Future[Seq[ReplayRecordResumen]] = {
    def buildQuery(
        param: SearchParam
    ): JsObject = {
      param match {
        case AndComposition(left, right) =>
          Json.obj(
            "$and" -> Seq(buildQuery(left), buildQuery(right))
          )
        case OrComposition(left, right) =>
          Json.obj("$or" -> JsArray(Seq(buildQuery(left), buildQuery(right))))
        case ByPlayer(discordID) =>
          Json.obj(
            "$or" -> Seq(
              Json.obj("winner.discordID" -> discordID),
              Json.obj("loser.discordID" -> discordID)
            )
          )
        case ByRace(race) =>
          Json.obj(
            "$or" -> Seq(
              Json.obj("winner.gamePlayer.race" -> race.str),
              Json.obj("loser.gamePlayer.race" -> race.str)
            )
          )
        case ByMatch(r1, r2) =>
          Json.obj(
            "$or" -> Seq(
              Json.obj(
                "$and" -> Seq(
                  Json.obj("winner.gamePlayer.race" -> r1.str),
                  Json.obj("loser.gamePlayer.race" -> r2.str)
                )
              ),
              Json.obj(
                "$and" -> Seq(
                  Json.obj("winner.gamePlayer.race" -> r2.str),
                  Json.obj("loser.gamePlayer.race" -> r1.str)
                )
              )
            )
          )
        case ByPlayerName(_) => Json.obj()
      }
    }

    collection.flatMap(
      _.find(buildQuery(param), Option.empty[ReplayRecordResumen])
        .cursor[ReplayRecordResumen](readPreference = ReadPreference.primary)
        .collect[Seq](-1, Cursor.FailOnError[Seq[ReplayRecordResumen]]())
    )
  }

  override def update(
      replayRecordResumen: ReplayRecordResumen
  ): Future[Boolean] =
    collection
      .flatMap(
        _.update(ordered = true).one(
          Json.obj("replayID" -> replayRecordResumen.replayID),
          replayRecordResumen,
          upsert = true
        )
      )
      .map(_.ok)
}
