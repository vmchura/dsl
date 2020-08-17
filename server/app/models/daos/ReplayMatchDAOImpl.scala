package models.daos

import java.util.UUID

import javax.inject.Inject
import models.ReplayRecord
import play.api.libs.json.{JsObject, Json}
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.Cursor
import reactivemongo.play.json.collection.JSONCollection

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
class ReplayMatchDAOImpl  @Inject() (val reactiveMongoApi: ReactiveMongoApi) extends ReplayMatchDAO {
  def collection: Future[JSONCollection] = reactiveMongoApi.database.map(_.collection("dsl.replays"))

  override def add(replayRecord: ReplayRecord): Future[Boolean] = collection.
    flatMap(_.update(ordered=true).
      one(Json.obj("replayID" -> replayRecord.replayID), replayRecord, upsert = true)).
    map(_.ok)

  override def markAsDisabled(replayID: UUID): Future[Boolean] = collection.
    flatMap(_.update(ordered=true).
      one(Json.obj("replayID" ->replayID),
        Json.obj("$set" -> Json.obj("enabled" -> false)), upsert = true)).
    map(_.ok)

  private def getMatchesByQuery(query: JsObject) = {
    collection.flatMap(_.find(query,Option.empty[ReplayRecord]).cursor[ReplayRecord]().collect[List](-1,Cursor.FailOnError[List[ReplayRecord]]()))

  }

  override def loadAllByTournament(tournamentID: Long): Future[Seq[ReplayRecord]] = {
    val query = Json.obj("tournamentID" -> tournamentID)
    getMatchesByQuery(query)
  }

  override def loadAllByMatch(tournamentID: Long, matchID: Long): Future[Seq[ReplayRecord]] = {
    val query = Json.obj("tournamentID" -> tournamentID,
      "matchID" -> matchID
    )
    getMatchesByQuery(query)

  }

  override def find(replayID: UUID): Future[Option[ReplayRecord]] = {
    val query = Json.obj("replayID" -> replayID)
    collection.flatMap(_.find(query,Option.empty[ReplayRecord]).one[ReplayRecord])

  }
}
