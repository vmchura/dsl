package models.daos

import java.util.UUID

import javax.inject.Inject
import models.{Participant, ParticipantPK}
import play.api.libs.json.Json
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.Cursor
import reactivemongo.play.json.collection.JSONCollection

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ParticipantDAOImpl  @Inject() (val reactiveMongoApi: ReactiveMongoApi) extends ParticipantDAO {
  def collection: Future[JSONCollection] = reactiveMongoApi.database.map(_.collection("dsl.participant"))

  override def find(participantPK: ParticipantPK): Future[Option[Participant]] = {
    val query = Json.obj("participantPK" -> participantPK)
    collection.flatMap(_.find(query,Option.empty[Participant]).one[Participant])
  }

  override def save(participant: Participant): Future[Boolean] = {
    collection.
      flatMap(_.update(ordered=true).
      one(Json.obj("participantPK" -> participant.participantPK), participant, upsert = true)).
      map(_.ok)

  }

  override def find(tournamentID: UUID): Future[Seq[Participant]] = {
    val query = Json.obj("participantPK.tournamentID" -> tournamentID)
    collection.flatMap(_.find(query,Option.empty[Participant]).cursor[Participant]().collect[List](-1,Cursor.FailOnError[List[Participant]]()))
  }
}
