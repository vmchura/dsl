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
      val pf = service.loadParticipant(ParticipantPK(1L,0L))
      val queryExecution = pf.map(p =>  assert(p.isEmpty))
      Await.result(queryExecution,5 seconds)
      queryExecution
    }
    "return a valid result after insertion" in {
      val primaryKeyToInsert = ParticipantPK(1L,1L)
      val queryExecution = for{
        pk <- Future.successful(primaryKeyToInsert)
        participant <- Future.successful(Participant(pk,"vmchq", None,None))
        insertion <- service.saveParticipant(participant)
        retrieve <- if(insertion) service.loadParticipant(pk) else Future.failed(new IllegalStateException("Not inserted"))
        deletion <- service.dropParticipant(pk)
      }yield{
        assertResult(Some(primaryKeyToInsert))(retrieve.map(_.participantPK))
        assert(deletion)
      }
      Await.result(queryExecution,5 seconds)
      queryExecution
    }

    "return a valid result based on tournament" in {
      val primaryKeyToInsert = ParticipantPK(1L,1L)
      val queryExecution = for{
        pk <- Future.successful(primaryKeyToInsert)
        participant <- Future.successful(Participant(pk,"vmchq",None,None))
        insertion <- service.saveParticipant(participant)
        retrieve <- if(insertion) service.loadParticipantsWithNoRelation(primaryKeyToInsert.challongeID) else Future.failed(new IllegalStateException("Not inserted"))
        deletion <- service.dropParticipant(pk)
      }yield{
        assertResult(List(primaryKeyToInsert))(retrieve.map(_.participantPK))
        assert(deletion)
      }
      Await.result(queryExecution,5 seconds)
      queryExecution
    }
    "return a valid result after update" in {
      val primaryKeyToInsert = ParticipantPK(1L,1L)
      val queryExecution = for{
        pk <- Future.successful(primaryKeyToInsert)
        participant <- Future.successful(Participant(pk,"vmchq",None,None))
        insertion <- service.saveParticipant(participant)
        update <- service.updateParticipantRelation(participant.copy(discordUserID = Some("vmchq"), userID = Some(UUID.randomUUID())))
        retrieveByTournament <- if(insertion && update) service.loadParticipantsWithNoRelation(primaryKeyToInsert.challongeID) else Future.failed(new IllegalStateException("Not inserted nor  updated"))
        retrieveByPK <- if(insertion && update) service.loadParticipant(primaryKeyToInsert) else Future.failed(new IllegalStateException("Not inserted nor updated"))
        deletion <- service.dropParticipant(pk)
      }yield{
        assertResult(List.empty[String])(retrieveByTournament.map(_.discordUserID))
        assertResult(Some("vmchq"))(retrieveByPK.flatMap(_.discordUserID))
        assert(deletion)
      }
      Await.result(queryExecution,5 seconds)
      queryExecution
    }

    "return a valid result using userID" in {
      val primaryKeyToInsert = ParticipantPK(1L,1L)
      val userID = UUID.randomUUID()
      val queryExecution = for{
        pk <- Future.successful(primaryKeyToInsert)
        participant <- Future.successful(Participant(pk,"vmchq",Some("vmchq"),Some(userID)))
        insertion <- service.saveParticipant(participant)
        retrieveByUserID <- if(insertion) service.loadParticipantByUserID(userID) else Future.failed(new IllegalStateException("Not inserted nor  updated"))
        deletion <- service.dropParticipant(pk)
      }yield{
        assertResult(List(primaryKeyToInsert))(retrieveByUserID.map(_.participantPK))
        assert(deletion)
      }
      Await.result(queryExecution,5 seconds)
      queryExecution
    }

    "return no participant by smurf" in {
      val smurfTest = "randomSmurf"
      val queryExecution = for{
        participant <- service.findBySmurf(smurfTest)
      }yield{
        assert(participant.isEmpty)
      }
      Await.result(queryExecution, 5 seconds)
      queryExecution

    }
    "add and delete smurf" in {
      val smurfTest = "randomSmurf"
      val pk = ParticipantPK(141341L,141L)

      val queryExecution = for{
        participant <- Future.successful(Participant(pk,"vmchq", None,None))
        inserted <- service.saveParticipant(participant)
        smurfAdded <- if(inserted) service.addSmurf(pk, smurfTest) else Future.successful(false)
        withSmurf <- service.loadParticipant(pk)
        smurfRemoved <- if(smurfAdded) service.removeSmurf(pk, smurfTest) else Future.successful(false)
        withNoSmurf <- service.loadParticipant(pk)
        deleted <- service.dropParticipant(pk)
      }yield{

        assert(deleted)
        assert(smurfRemoved)
        assert(withSmurf.fold(false)(_.smurfs.contains(smurfTest)))
        assert(withNoSmurf.fold(false)(! _.smurfs.contains(smurfTest)))
      }
      Await.result(queryExecution, 5 seconds)
      queryExecution
    }
    "find by smurf" in {
      val smurfTest = "randomSmurf"
      val pk = ParticipantPK(141341L,141L)

      val queryExecution = for{
        participant <- Future.successful(Participant(pk,"vmchq",Some("qwdasd"),Some(UUID.randomUUID())))
        inserted <- service.saveParticipant(participant)
        smurfAdded <- if(inserted) service.addSmurf(pk, smurfTest) else Future.successful(false)
        withSmurf <- service.findBySmurf(smurfTest)
        deleted <- service.dropParticipant(pk)
      }yield{

        assert(deleted)
        assertResult(List(participant.participantPK))(withSmurf.map(_.participantPK))
      }
      Await.result(queryExecution, 5 seconds)
      queryExecution

    }



  }

}
