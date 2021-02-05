package models.daos.teamsystem

import com.google.inject.Inject
import models.teamsystem.{RequestJoin, RequestJoinID, TeamID}
import play.api.libs.json._
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.{Cursor, ReadPreference}
import reactivemongo.play.json._
import reactivemongo.play.json.collection.{JSONCollection, _}
import shared.models.DiscordID

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RequestDAOImpl @Inject() (val reactiveMongoApi: ReactiveMongoApi)
    extends RequestDAO {
  import models.ModelsJsonImplicits._
  def collection: Future[JSONCollection] =
    reactiveMongoApi.database.map(_.collection("teamsystem.request"))

  override def loadRequest(
      requestID: RequestJoinID
  ): Future[Option[RequestJoin]] =
    collection
      .flatMap(
        _.find(
          Json.obj("requestID" -> requestID),
          Option.empty[RequestJoin]
        ).one[RequestJoin]
      )

  override def requestsToTeam(
      teamID: TeamID
  ): Future[Seq[RequestJoin]] =
    collection
      .flatMap(
        _.find(Json.obj("teamID" -> teamID), Option.empty[RequestJoin])
          .cursor[RequestJoin](readPreference = ReadPreference.primary)
          .collect[Seq](-1, Cursor.FailOnError[Seq[RequestJoin]]())
      )

  override def addRequest(
      request: RequestJoin
  ): Future[RequestJoinID] = {
    collection
      .flatMap(
        _.insert(ordered = true).one(request)
      )
      .map(_ => request.requestID)
  }

  override def removeRequest(requestID: RequestJoinID): Future[Boolean] =
    collection
      .flatMap(
        _.delete(ordered = true).one(Json.obj("requestID" -> requestID))
      )
      .map(_.ok)

  override def requestsFromUser(
      discordID: DiscordID
  ): Future[Seq[RequestJoin]] =
    collection
      .flatMap(
        _.find(Json.obj("from" -> discordID), Option.empty[RequestJoin])
          .cursor[RequestJoin](readPreference = ReadPreference.primary)
          .collect[Seq](-1, Cursor.FailOnError[Seq[RequestJoin]]())
      )
}
