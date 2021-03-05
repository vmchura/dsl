package models.daos.teamsystem
import com.google.inject.Inject
import models.teamsystem.TeamReplayInfo
import play.api.libs.json._
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.play.json.collection.JSONCollection
import shared.models.ReplayTeamID

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class TeamReplayDAOImpl @Inject() (val reactiveMongoApi: ReactiveMongoApi)
    extends TeamReplayDAO {
  import models.ModelsJsonImplicits._
  def collection: Future[JSONCollection] =
    reactiveMongoApi.database.map(_.collection("teamsystem.teamreplayinfo"))
  override def save(replayInfo: TeamReplayInfo): Future[TeamReplayInfo] =
    collection
      .flatMap(
        _.insert(ordered = true).one(replayInfo)
      )
      .map(_ => replayInfo)

  override def load(
      replayTeamID: ReplayTeamID
  ): Future[Option[TeamReplayInfo]] =
    collection
      .flatMap(
        _.find(
          Json.obj("replayTeamID" -> replayTeamID),
          Option.empty[TeamReplayInfo]
        ).one[TeamReplayInfo]
      )

}
