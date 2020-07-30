package models.daos

import java.util.UUID

import javax.inject.Inject
import models.{MatchResult, ReplayRecord}
import play.api.libs.json.{JsObject, Json}
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.Cursor
import reactivemongo.play.json.collection.JSONCollection

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class MatchResultDAOImpl  @Inject()(val reactiveMongoApi: ReactiveMongoApi) extends MatchResultDAO {
  def collection: Future[JSONCollection] = reactiveMongoApi.database.map(_.collection("dsl.matchresult"))

  override def save(matchResultRecord: MatchResult): Future[Boolean] = collection.
    flatMap(_.update(ordered=true).
      one(Json.obj("matchResultID" -> matchResultRecord.matchResultID), matchResultRecord, upsert = true)).
    map(_.ok)

  override def find(matchResultID: UUID): Future[Option[MatchResult]] = {
    val query = Json.obj("matchResultID" -> matchResultID)
    collection.flatMap(_.find(query,Option.empty[MatchResult]).one[MatchResult])
  }
}
