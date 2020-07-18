package jobs
import models.services.{ParticipantsService, TournamentService}

import scala.concurrent.ExecutionContext.Implicits.global
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.language.postfixOps
class TournamentBuilderTest extends PlaySpec with GuiceOneAppPerSuite {
  val job: TournamentBuilder = app.injector.instanceOf(classOf[TournamentBuilder])
  val tournamentService: TournamentService = app.injector.instanceOf(classOf[TournamentService])
  val participantsService: ParticipantsService = app.injector.instanceOf(classOf[ParticipantsService])

  "A TournamentBuilder job" should {
    "build correctly a tournament" in {
      val queryExecution = for {
        creation          <- job.buildTournament("728442814832312372","DeathfateStarLeague")
        tournamentLoaded <- creation match {
          case Left(_) => Future.successful(None)
          case Right(tournament) => tournamentService.loadTournament(tournament.challongeID)
        }
        tournamentRemove  <- creation match {
          case Left(_) => Future.successful(false)
          case Right(tournament) => tournamentService.dropTournament(tournament.challongeID)
        }

        participants <- creation match {
          case Left(_) => Future.successful(Left(UnknowError("error creation tournament")))
          case Right(tournament) => job.getParticipantsUsers(tournament.challongeID)
        }

        participantsRemove <- participants match {
          case Left(_) => Future.successful(false)
          case Right((_, particpants, _)) => Future.sequence(particpants.map(_.participantPK).map(participantsService.dropParticipant)).map(_.forall(q => q))
        }
      }yield {
        assert(tournamentRemove)
        assert(participantsRemove)
        assertResult(Some(8415514))(tournamentLoaded.map(_.challongeID))
        creation
      }
      Await.result(queryExecution, 30 seconds)
      val testExecution = queryExecution.map{
        case Left(error) =>
          fail("unexpected error",error)
        case Right(_) =>
          succeed
      }
      Await.result(testExecution, 30 seconds)
      testExecution
    }
    "throw tournament created" in {
      val queryExecution = for {
        creation          <- job.buildTournament("728442814832312372","DeathfateStarLeague")
        tournamentLoaded <- creation match {
          case Left(_) => Future.successful(None)
          case Right(tournament) => tournamentService.loadTournament(tournament.challongeID)
        }
        participants <- creation match {
          case Left(_) => Future.successful(Left(UnknowError("error creation tournament")))
          case Right(tournament) => job.getParticipantsUsers(tournament.challongeID)
        }


        participantsRemove <- participants match {
          case Left(_) => Future.successful(false)
          case Right((_, particpants,_)) => Future.sequence(particpants.map(_.participantPK).map(participantsService.dropParticipant)).map(_.forall(q => q))
        }
        creation2          <- job.buildTournament("728442814832312372","DeathfateStarLeague")
        tournamentRemove  <- creation match {
          case Left(_) => Future.successful(false)
          case Right(tournament) => tournamentService.dropTournament(tournament.challongeID)
        }
      }yield {
        assert(tournamentRemove)
        assert(participantsRemove)
        assertResult(Some(8415514))(tournamentLoaded.map(_.challongeID))
        creation2
      }
      Await.result(queryExecution, 30 seconds)
      val testResultExecution = queryExecution.map{
        case Left(_: TournamentAlreadyCreated) => succeed
        case Left(error) => fail("Expected other type of error", error)
        case Right(_) => fail("Finished correctly, expected error")
      }
      Await.result(testResultExecution, 30 seconds)
      testResultExecution
    }
    "throw discord error" in {
      val queryExecution = for {
        creation          <- job.buildTournament("728442814832312372_","DeathfateStarLeague")
      }yield {
        creation
      }
      Await.result(queryExecution, 30 seconds)
      val testExecution = queryExecution.map{
        case Left(_: CannotAccesDiscordGuild) => succeed
        case Left(error) => fail("Expected other type of error", error)
        case Right(_) => fail("Finished correctly, expected error")
      }
      Await.result(testExecution, 30 seconds)
      testExecution
    }
    "throw challonge error" in {
      val queryExecution = for {
        creation          <- job.buildTournament("728442814832312372","DeathfateStarLeague_")
      }yield {
        creation
      }
      Await.result(queryExecution, 30 seconds)
      val testExecution = queryExecution.map{
        case Left(_: CannontAccessChallongeTournament) => succeed
        case Left(error) => fail("Expected other type of error", error)
        case Right(_) => fail("Finished correctly, expected error")
      }
      Await.result(testExecution, 30 seconds)
      testExecution
    }
  }

}
