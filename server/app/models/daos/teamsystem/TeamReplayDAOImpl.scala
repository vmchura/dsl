package models.daos.teamsystem
import com.google.inject.Inject
import models.teamsystem.TeamReplayInfo
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.play.json.collection.JSONCollection

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class TeamReplayDAOImpl @Inject() (val reactiveMongoApi: ReactiveMongoApi)
    extends TeamReplayDAO {
  def collection: Future[JSONCollection] =
    reactiveMongoApi.database.map(_.collection("teamsystem.teamreplayinfo"))
  override def save(replayInfo: TeamReplayInfo): Future[TeamReplayInfo] =
    collection
      .flatMap(
        _.insert(ordered = true).one(replayInfo)
      )
      .map(_ => replayInfo)
}
