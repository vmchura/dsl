package models.daos.teamsystem
import com.google.inject.Inject
import play.api.libs.json.Json
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.play.json.collection.JSONCollection
import shared.models.{ReplayTeamID, ReplayTeamRecord}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class TeamMetaReplayTeamDAOImpl @Inject() (
    val reactiveMongoApi: ReactiveMongoApi
) extends TeamMetaReplayTeamDAO {
  import models.ModelsJsonImplicits._

  def collection: Future[JSONCollection] =
    reactiveMongoApi.database.map(_.collection("teamsystem.teammetareplay"))
  override def save(replay: ReplayTeamRecord): Future[Boolean] = {
    collection
      .flatMap(
        _.insert(ordered = true).one(replay)
      )
      .map(_.ok)

  }

  override def isRegistered(hash: String): Future[Boolean] = {

    val query = Json.obj("replayMD5Hash" -> hash)
    collection
      .flatMap(
        _.find(query, Option.empty[ReplayTeamRecord]).one[ReplayTeamRecord]
      )
      .map(_.nonEmpty)
  }
  override def load(
      replayTeamID: ReplayTeamID
  ): Future[Option[ReplayTeamRecord]] = {
    val query = Json.obj("id" -> replayTeamID)

    collection.flatMap(
      _.find(query, Option.empty[ReplayTeamRecord]).one[ReplayTeamRecord]
    )
  }
}
