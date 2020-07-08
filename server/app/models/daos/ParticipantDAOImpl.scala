package models.daos

import javax.inject.Inject
import models.{Participant, ParticipantPK}
import play.api.libs.json.Json
import play.modules.reactivemongo.ReactiveMongoApi
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
}
