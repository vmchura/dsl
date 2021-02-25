package models.daos.teamsystem

import models.teamsystem.{PendingSmurf, TeamID}
import play.api.libs.json.Json
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.Cursor
import reactivemongo.play.json.collection.JSONCollection
import shared.models.DiscordID

import javax.inject.Inject
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class TeamUserSmurfPendingDAOImpl @Inject() (
    val reactiveMongoApi: ReactiveMongoApi,
    teamDAO: TeamDAO
) extends TeamUserSmurfPendingDAO {
  import models.ModelsJsonImplicits._
  def collection: Future[JSONCollection] =
    reactiveMongoApi.database.map(_.collection("teamsystem.smurfpending"))

  override def add(
      pendingSmurf: PendingSmurf
  ): Future[Boolean] =
    collection
      .flatMap(
        _.insert(ordered = true).one(pendingSmurf)
      )
      .map(_.ok)

  override def load(
      discordID: DiscordID
  ): Future[Seq[PendingSmurf]] =
    collection.flatMap(
      _.find(Json.obj("discordID" -> discordID), Option.empty[PendingSmurf])
        .cursor[PendingSmurf]()
        .collect[List](-1, Cursor.FailOnError[List[PendingSmurf]]())
    )

  override def loadFromTeam(
      teamID: TeamID
  ): Future[Seq[PendingSmurf]] =
    teamDAO.loadTeam(teamID).flatMap {
      case Some(team) =>
        Future.traverse(team.members.map(_.userID))(load).map(_.flatten)
      case None => Future.successful(Nil)
    }
}
