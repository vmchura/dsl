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
class TeamDAOImpl @Inject() (val reactiveMongoApi: ReactiveMongoApi)
    extends TeamDAO {

  def collection: Future[JSONCollection] =
    reactiveMongoApi.database.map(_.collection("teamsystem.team"))

  override def save(
      userID: models.DiscordID,
      teamName: String
  ): Future[TeamID] = {
    val id = TeamID(UUID.randomUUID())
    collection
      .flatMap(
        _.insert(ordered = true).one(Team(id, teamName, userID, Nil))
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
}
