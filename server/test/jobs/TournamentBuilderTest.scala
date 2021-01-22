package jobs
import database.EmptyDBBeforeEach
import models.Tournament
import models.services.{ParticipantsService, TournamentService}

import scala.concurrent.ExecutionContext.Implicits.global
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.language.postfixOps
class TournamentBuilderTest
    extends PlaySpec
    with GuiceOneAppPerSuite
    with EmptyDBBeforeEach {
  val job: TournamentBuilder =
    app.injector.instanceOf(classOf[TournamentBuilder])
  val tournamentService: TournamentService =
    app.injector.instanceOf(classOf[TournamentService])
  val participantsService: ParticipantsService =
    app.injector.instanceOf(classOf[ParticipantsService])
  implicit class eitherError[A](either: Either[JobError, A]) {
    def withFailure[E <: JobError]: Future[A] =
      either match {
        case Left(e)  => Future.failed(e)
        case Right(a) => Future.successful(a)
      }
  }

  "A TournamentBuilder job" should {
    "insertion and load of tournament" in {
      val queryExecution = for {
        creation <- tournamentService.saveTournament(
          Tournament(1L, "1Str", "DServer", "TorunamentName", active = true)
        )
        deletion <-
          if (creation) tournamentService.dropTournament(1L)
          else Future.successful(false)

      } yield {
        assert(deletion)
      }
      Await.result(queryExecution, 30 seconds)
      queryExecution
    }

    "build correctly a tournament" in {
      val queryExecution = for {
        creation <-
          job.buildTournament("736004357866389584", "DeathfateStarLeague")
        tournament <- creation.withFailure
        tournamentLoadedOpt <-
          tournamentService.loadTournament(tournament.challongeID)
        tournamentLoaded <- tournamentLoadedOpt.withFailure(
          TournamentNotBuild(tournament.challongeID)
        )
        participantsJob <- job.getParticipantsUsers(tournament.challongeID)
        (_, participants, _) <- participantsJob.withFailure
        participantsRemove <- Future.sequence(
          participants
            .map(_.participantPK)
            .map(participantsService.dropParticipant)
        )
        tournamentRemove <-
          tournamentService.dropTournament(tournament.challongeID)

      } yield {
        assert(tournamentRemove)
        assert(participantsRemove.forall(x => x))
        assertResult(8415514)(tournamentLoaded.challongeID)
        creation
      }
      Await.result(queryExecution, 30 seconds)
      val testExecution = queryExecution.map {
        case Left(error) =>
          fail("unexpected error", error)
        case Right(_) =>
          succeed
      }
      Await.result(testExecution, 30 seconds)
      testExecution
    }
    "throw tournament created" in {
      val queryExecution = for {
        creation <-
          job.buildTournament("736004357866389584", "DeathfateStarLeague")
        tournamentLoaded <- creation match {
          case Left(_) => Future.successful(None)
          case Right(tournament) =>
            tournamentService.loadTournament(tournament.challongeID)
        }
        participants <- creation match {
          case Left(_) =>
            Future.successful(
              Left(UnknowTournamentBuilderError("error creation tournament"))
            )
          case Right(tournament) =>
            job.getParticipantsUsers(tournament.challongeID)
        }

        participantsRemove <- participants match {
          case Left(_) => Future.successful(false)
          case Right((_, particpants, _)) =>
            Future
              .sequence(
                particpants
                  .map(_.participantPK)
                  .map(participantsService.dropParticipant)
              )
              .map(_.forall(q => q))
        }
        creation2 <-
          job.buildTournament("736004357866389584", "DeathfateStarLeague")
        tournamentRemove <- creation match {
          case Left(_) => Future.successful(false)
          case Right(tournament) =>
            tournamentService.dropTournament(tournament.challongeID)
        }
      } yield {
        assert(tournamentRemove)
        assert(participantsRemove)
        assertResult(Some(8415514))(tournamentLoaded.map(_.challongeID))
        creation2
      }
      Await.result(queryExecution, 30 seconds)
      val testResultExecution = queryExecution.map {
        case Left(_: TournamentAlreadyCreated) => succeed
        case Left(error)                       => fail("Expected other type of error", error)
        case Right(_)                          => fail("Finished correctly, expected error")
      }
      Await.result(testResultExecution, 30 seconds)
      testResultExecution
    }
    "throw discord error" in {
      val queryExecution = for {
        creation <- job.buildTournament("randomID", "DeathfateStarLeague")
      } yield {
        creation
      }
      Await.result(queryExecution, 30 seconds)
      val testExecution = queryExecution.map {
        case Left(_: CannotAccesDiscordGuild) => succeed
        case Left(error)                      => fail("Expected other type of error", error)
        case Right(_)                         => fail("Finished correctly, expected error")
      }
      Await.result(testExecution, 30 seconds)
      testExecution
    }
    "throw challonge error" in {
      val queryExecution = for {
        creation <-
          job.buildTournament("728442814832312372", "DeathfateStarLeague_")
      } yield {
        creation
      }
      Await.result(queryExecution, 30 seconds)
      val testExecution = queryExecution.map {
        case Left(_: CannontAccessChallongeTournament) => succeed
        case Left(error)                               => fail("Expected other type of error", error)
        case Right(_)                                  => fail("Finished correctly, expected error")
      }
      Await.result(testExecution, 30 seconds)
      testExecution
    }
  }

}
