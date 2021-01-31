package models.daos.teamsystem

import com.google.inject.Inject
import models.teamsystem.{Invitation, InvitationID}
import play.api.libs.json.{JsObject, Json}
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.{Cursor, ReadPreference}
import reactivemongo.play.json.collection.JSONCollection

import java.util.UUID
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import reactivemongo.play.json._
import collection._
import shared.models.DiscordID
class InvitationDAOImpl @Inject() (val reactiveMongoApi: ReactiveMongoApi)
    extends InvitationDAO {
  import models.ModelsJsonImplicits._
  def collection: Future[JSONCollection] =
    reactiveMongoApi.database.map(_.collection("teamsystem.invitation"))

  private def invitationQueryExecuter(
      query: JsObject
  ): Future[Seq[Invitation]] = {
    collection
      .flatMap(
        _.find(query, Option.empty[Invitation])
          .cursor[Invitation](readPreference = ReadPreference.primary)
          .collect[Seq](-1, Cursor.FailOnError[Seq[Invitation]]())
      )
  }
  override def invitationsToUser(
      userID: DiscordID
  ): Future[Seq[Invitation]] =
    invitationQueryExecuter(Json.obj("to" -> userID))

  override def invitationsFromUser(
      userID: DiscordID
  ): Future[Seq[Invitation]] =
    invitationQueryExecuter(Json.obj("from" -> userID))

  override def addInvitation(invitation: Invitation): Future[InvitationID] = {
    collection
      .flatMap(
        _.insert(ordered = true).one(invitation)
      )
      .map(_ => invitation.invitationID)
  }

  override def removeInvitation(invitationID: InvitationID): Future[Boolean] =
    collection
      .flatMap(
        _.delete(ordered = true).one(Json.obj("invitationID" -> invitationID))
      )
      .map(_.ok)

  override def loadInvitation(
      invitationID: InvitationID
  ): Future[Option[Invitation]] =
    collection
      .flatMap(
        _.find(
          Json.obj("invitationID" -> invitationID),
          Option.empty[Invitation]
        ).one[Invitation]
      )

}
