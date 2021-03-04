package models.daos.teamsystem

import models.teamsystem.{PendingSmurf, TeamID}
import play.api.libs.json.Json
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.Cursor
import reactivemongo.play.json.collection.JSONCollection
import shared.models.{DiscordID, ReplayTeamID}

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

  override def load(replayTeamID: ReplayTeamID): Future[Option[PendingSmurf]] =
    collection.flatMap(
      _.find(
        Json.obj("replayTeamID" -> replayTeamID),
        Option.empty[PendingSmurf]
      ).one[PendingSmurf]
    )

  override def remove(replayTeamID: ReplayTeamID): Future[Boolean] =
    collection.flatMap(
      _.delete(ordered = true)
        .one(
          Json.obj("replayTeamID" -> replayTeamID)
        )
        .map(_.ok)
    )

  override def removeRelated(replayTeamID: ReplayTeamID): Future[Boolean] = {
    for {
      pending <- load(replayTeamID)
      related <- pending.fold(Future.successful(false))(p =>
        collection
          .flatMap(
            _.delete(ordered = true)
              .one(Json.obj("smurf" -> p.smurf, "discordID" -> p.discordID))
          )
          .map(_.ok)
      )
    } yield {
      related
    }
  }

  override def loadRelated(
      replayTeamID: ReplayTeamID
  ): Future[Seq[PendingSmurf]] =
    for {
      pending <- load(replayTeamID)
      related <- pending.fold(Future.successful(List.empty[PendingSmurf]))(p =>
        collection.flatMap(
          _.find(
            Json.obj("smurf" -> p.smurf, "discordID" -> p.discordID),
            Option.empty[PendingSmurf]
          ).cursor[PendingSmurf]()
            .collect[List](-1, Cursor.FailOnError[List[PendingSmurf]]())
        )
      )
    } yield {
      related
    }
}
