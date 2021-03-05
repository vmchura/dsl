package models.daos.teamsystem
import com.google.inject.Inject
import models.teamsystem.{Points, TeamID}
import play.api.libs.json.Json
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.{Cursor, ReadPreference}
import reactivemongo.play.json.collection.JSONCollection
import shared.models.ReplayTeamID

import java.util.Date
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
class PointsDAOImpl @Inject() (val reactiveMongoApi: ReactiveMongoApi)
    extends PointsDAO {
  import models.ModelsJsonImplicits._
  def collection: Future[JSONCollection] =
    reactiveMongoApi.database.map(_.collection("teamsystem.points"))

  override def save(points: Points): Future[Boolean] =
    collection
      .flatMap(
        _.insert(ordered = true).one(points)
      )
      .map(_.ok)

  override def load(replayTeamID: ReplayTeamID): Future[Option[Points]] =
    collection
      .flatMap(
        _.find(
          Json.obj("replayTeamID" -> replayTeamID),
          Option.empty[Points]
        ).one[Points]
      )

  override def load(teamID: TeamID, enabled: Boolean): Future[Seq[Points]] =
    collection
      .flatMap(
        _.find(
          Json.obj("teamID" -> teamID, "enabled" -> enabled),
          Option.empty[Points]
        ).cursor[Points](readPreference = ReadPreference.primary)
          .collect[Seq](-1, Cursor.FailOnError[Seq[Points]]())
      )

  override def disableByTime(threadsHold: Date): Future[Int] =
    collection
      .flatMap(
        _.update(ordered = true).one(
          Json.obj("date" -> Json.obj("$lt" -> threadsHold)),
          Json.obj("enabled" -> false),
          upsert = true
        )
      )
      .map(_.nModified)
}
