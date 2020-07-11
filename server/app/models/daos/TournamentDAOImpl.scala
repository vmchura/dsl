package models.daos

import java.util.UUID

import javax.inject.Inject
import models.Tournament
import play.api.libs.json.Json
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.Cursor
import reactivemongo.play.json.collection.JSONCollection

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class TournamentDAOImpl  @Inject() (val reactiveMongoApi: ReactiveMongoApi) extends TournamentDAO {
  def collection: Future[JSONCollection] = reactiveMongoApi.database.map(_.collection("dsl.tournament"))

  override def save(tournament: Tournament): Future[Boolean] =
    for{
      loaded <- load(tournament.tournamentID)
      newInsertion <- loaded.fold(collection.
        flatMap(_.update(ordered=true).
          one(Json.obj("tournamentID" -> tournament.tournamentID), tournament, upsert = true)).
        map(_.ok))(_ => Future.successful(false))
    }yield {
      newInsertion
    }


  override def load(tournamentID: UUID): Future[Option[Tournament]] = {
    val query = Json.obj("tournamentID" -> tournamentID)
    collection.flatMap(_.find(query,Option.empty[Tournament]).one[Tournament])

  }

  override def all(): Future[Seq[Tournament]] =
    collection.flatMap(_.find(Json.obj(),Option.empty[Tournament]).cursor[Tournament]().collect[List](-1,Cursor.FailOnError[List[Tournament]]()))

  override def remove(tournamentID: UUID): Future[Boolean] = {
    val query = Json.obj("tournamentID" -> tournamentID)
    collection.flatMap(_.delete(ordered=true).one(query).map(_.ok))
  }
}