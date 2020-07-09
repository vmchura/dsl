package models.services

import java.util.UUID

import models.{Participant, ParticipantPK}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import org.scalatestplus.play._

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.language.postfixOps
class ParticipantsServiceImplTest extends PlaySpec with GuiceOneAppPerSuite{

  val service: ParticipantsService = app.injector.instanceOf(classOf[ParticipantsService])
  "A participant service" should {
    "return an empty result of unknown participant" in {
      val pf = service.loadParticipant(ParticipantPK(UUID.randomUUID(),0L))
      val queryExecution = pf.map(p =>  assert(p.isEmpty))
      Await.result(queryExecution,5 seconds)
      queryExecution
    }
    "return a valid result after insertion" in {
      val primaryKeyToInsert = ParticipantPK(UUID.randomUUID(),1L)
      val queryExecution = for{
        pk <- Future.successful(primaryKeyToInsert)
        participant <- Future.successful(Participant(pk,None,None))
        insertion <- service.saveParticipant(participant)
        retrieve <- if(insertion) service.loadParticipant(pk) else Future.failed(new IllegalStateException("Not inserted"))
      }yield{
        assertResult(Some(primaryKeyToInsert))(retrieve.map(_.participantPK))
      }
      Await.result(queryExecution,5 seconds)
      queryExecution
    }

    "return a valid result based on tournament" in {
      val primaryKeyToInsert = ParticipantPK(UUID.randomUUID(),1L)
      val queryExecution = for{
        pk <- Future.successful(primaryKeyToInsert)
        participant <- Future.successful(Participant(pk,None,None))
        insertion <- service.saveParticipant(participant)
        retrieve <- if(insertion) service.loadParticipantsWithNoRelation(primaryKeyToInsert.tournamentID) else Future.failed(new IllegalStateException("Not inserted"))
      }yield{
        assertResult(List(primaryKeyToInsert))(retrieve.map(_.participantPK))
      }
      Await.result(queryExecution,5 seconds)
      queryExecution
    }
    "return a valid result after update" in {
      val primaryKeyToInsert = ParticipantPK(UUID.randomUUID(),1L)
      val queryExecution = for{
        pk <- Future.successful(primaryKeyToInsert)
        participant <- Future.successful(Participant(pk,None,None))
        insertion <- service.saveParticipant(participant)
        update <- service.updateParticipantRelation(participant.copy(discordUserID = Some("vmchq"), userID = Some(UUID.randomUUID())))
        retrieveByTournament <- if(insertion && update) service.loadParticipantsWithNoRelation(primaryKeyToInsert.tournamentID) else Future.failed(new IllegalStateException("Not inserted nor  updated"))
        retrieveByPK <- if(insertion && update) service.loadParticipant(primaryKeyToInsert) else Future.failed(new IllegalStateException("Not inserted nor updated"))
      }yield{
        assertResult(List.empty[String])(retrieveByTournament.map(_.discordUserID))
        assertResult(Some("vmchq"))(retrieveByPK.flatMap(_.discordUserID))
      }
      Await.result(queryExecution,5 seconds)
      queryExecution
    }

  }

}
