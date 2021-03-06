package models.daos.teamsystem

import com.google.inject.Inject
import models.teamsystem.{Member, Team, TeamID}
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.play.json.collection.JSONCollection

import java.util.UUID
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.libs.json._
import reactivemongo.api._
import reactivemongo.play.json._
import collection._
import shared.models.DiscordID
class TeamDAOImpl @Inject() (val reactiveMongoApi: ReactiveMongoApi)
    extends TeamDAO {
  import models.ModelsJsonImplicits._
  def collection: Future[JSONCollection] =
    reactiveMongoApi.database.map(_.collection("teamsystem.team"))

  override def save(
      userID: DiscordID,
      teamName: String
  ): Future[TeamID] = {
    val id = TeamID(UUID.randomUUID())
    collection
      .flatMap(
        _.insert(ordered = true).one(Team(id, teamName, userID, Nil, None))
      )
      .map(_ => id)

  }

  override def loadTeams(): Future[Seq[Team]] =
    collection.flatMap(
      _.find(Json.obj(), Option.empty[Team])
        .cursor[Team](readPreference = ReadPreference.primary)
        .collect[Seq](-1, Cursor.FailOnError[Seq[Team]]())
    )

  override def addMemberTo(member: Member, teamID: TeamID): Future[Boolean] =
    collection.flatMap(
      _.update(ordered = true)
        .one(
          Json.obj("teamID" -> teamID),
          Json.obj("$push" -> Json.obj("members" -> member)),
          upsert = true
        )
        .map(_.ok)
    )

  override def removeTeam(teamID: TeamID): Future[Boolean] =
    collection
      .flatMap(
        _.delete(ordered = true).one(Json.obj("teamID" -> teamID))
      )
      .map(_.ok)

  override def removeMember(
      userID: DiscordID,
      teamID: TeamID
  ): Future[Boolean] =
    collection.flatMap(
      _.update(ordered = true)
        .one(
          Json.obj("teamID" -> teamID),
          Json.obj(
            "$pull" -> Json.obj("members" -> Json.obj("userID" -> userID))
          ),
          upsert = true
        )
        .map(_.ok)
    )

  override def loadTeam(teamID: TeamID): Future[Option[Team]] =
    collection.flatMap(
      _.find(Json.obj("teamID" -> teamID), Option.empty[Team]).one[Team]
    )

  override def updateTeamLogo(teamID: TeamID, newURL: String): Future[Boolean] =
    collection
      .flatMap(
        _.update(ordered = true)
          .one(
            Json.obj("teamID" -> teamID),
            Json.obj("$set" -> Json.obj("logo" -> Some(newURL))),
            upsert = true
          )
      )
      .map(_.ok)
}
