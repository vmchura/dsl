package models.services
import java.util.UUID

import models.{ChallongeTournament, Participant, ParticipantPK, Tournament}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Span}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import org.scalatestplus.play._

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.language.postfixOps
class TournamentServiceImplTest
    extends PlaySpec
    with GuiceOneAppPerSuite
    with ScalaFutures {
  implicit override val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = Span(100, Seconds), interval = Span(1, Seconds))

  private val service: TournamentService =
    app.injector.instanceOf(classOf[TournamentService])
  private val challongeTournamentService: ChallongeTournamentService =
    app.injector.instanceOf(classOf[ChallongeTournamentService])

  private val participantService =
    app.injector.instanceOf(classOf[ParticipantsService])

  private val tournamentActive = Tournament(0L, "-", "0L", "D0L", active = true)
  private val tournamentNotActive =
    Tournament(1L, "-", "1L", "D1L", active = false)

  private val disuser1 = UUID.randomUUID()
  private val disuser2 = UUID.randomUUID()

  private val player1_active = Participant(
    ParticipantPK(tournamentActive.challongeID, 0L),
    "player1",
    Some("player1"),
    Some(disuser1)
  )
  private val player1_notactive = Participant(
    ParticipantPK(tournamentNotActive.challongeID, 1L),
    "player1",
    Some("player1"),
    Some(disuser1)
  )

  private val player2_active = Participant(
    ParticipantPK(tournamentActive.challongeID, 2L),
    "player2",
    Some("player2"),
    Some(disuser2)
  )
  private val player2_notactive = Participant(
    ParticipantPK(tournamentNotActive.challongeID, 3L),
    "player2",
    Some("player2"),
    Some(disuser2)
  )

  private def tournamentsSatisfiesPredicate(
      filter: Tournament => Boolean
  )(tournaments: Seq[Tournament]) = {
    def sorted(t: Seq[Tournament]) =
      t.toList.filter(filter).sortBy(_.challongeID)
    sorted(tournaments) == sorted(List(tournamentActive, tournamentNotActive))
  }
  private def allTournaments(tournaments: Seq[Tournament]) =
    tournamentsSatisfiesPredicate(_ => true)(tournaments)
  private def allActiveTournaments(tournaments: Seq[Tournament]) =
    tournamentsSatisfiesPredicate(_.active)(tournaments)

  "A tournament service" should {
    "return an empty result of unknown tournament" in {
      val pf = service.loadTournament(1L)
      val queryExecution = pf.map(p => assert(p.isEmpty))
      Await.result(queryExecution, 5 seconds)
      queryExecution
    }
    "return false after insertion of the same tournament" in {
      val queryExecution = for {
        in1 <- service.saveTournament(tournamentActive)
        in2 <- service.saveTournament(tournamentActive)
        removed <- service.dropTournament(tournamentActive.challongeID)
      } yield {
        assert(in1 && !in2 && removed)
      }
      Await.result(queryExecution, 5 seconds)
      queryExecution
    }
    "return  2 tournaments after 2 insertions" in {

      val queryExecution = for {
        _ <- service.saveTournament(tournamentActive)
        _ <- service.saveTournament(tournamentNotActive)
        tournaments <- service.findAllTournaments()
        equals <- Future.successful(allTournaments(tournaments))
        removed <- Future.sequence(
          List(tournamentActive, tournamentNotActive)
            .map(_.challongeID)
            .map(service.dropTournament)
        )
      } yield {
        assert(removed.forall(q => q))
        assert(equals)
      }
      Await.result(queryExecution, 5 seconds)
      queryExecution
    }
    "return  1 tournament active after 2 insertions" in {

      val queryExecution = for {
        _ <- service.saveTournament(tournamentActive)
        _ <- service.saveTournament(tournamentNotActive)
        tournaments <- service.findAllActiveTournaments()
        equals <- Future.successful(allActiveTournaments(tournaments))
        removed <- Future.sequence(
          List(tournamentActive).map(_.challongeID).map(service.dropTournament)
        )
      } yield {
        assert(removed.forall(q => q) && equals)
      }
      Await.result(queryExecution, 5 seconds)
      queryExecution
    }
    "return 1 tournament active by player" in {
      val queryExecution = for {
        _ <- participantService.saveParticipant(player1_active)
        _ <- participantService.saveParticipant(player1_notactive)
        _ <- participantService.saveParticipant(player2_active)
        _ <- participantService.saveParticipant(player2_notactive)
        _ <- service.saveTournament(tournamentActive)
        _ <- service.saveTournament(tournamentNotActive)
        tAll1 <- service.findAllTournamentsByPlayer(disuser1)
        tActive1 <- service.findAllActiveTournamentsByPlayer(disuser1)
        tAll2 <- service.findAllTournamentsByPlayer(disuser2)
        tActive2 <- service.findAllActiveTournamentsByPlayer(disuser2)
        _ <- service.dropTournament(tournamentActive.challongeID)
        _ <- service.dropTournament(tournamentNotActive.challongeID)
        deletion <- Future.sequence(
          List(
            player1_active,
            player1_notactive,
            player2_active,
            player2_notactive
          ).map(_.participantPK).map(participantService.dropParticipant)
        )
      } yield {
        assert(allTournaments(tAll1))
        assert(allTournaments(tAll2))

        assert(allActiveTournaments(tActive1))
        assert(allActiveTournaments(tActive2))

        assert(deletion.forall(i => i))
      }
      Await.result(queryExecution, 5 seconds)
      queryExecution
    }

    "Insertion legacy" in {
      val load: String => Future[Option[ChallongeTournament]] =
        challongeTournamentService.findChallongeTournament(
          "699897834685857792",
          None
        )

      val save: Future[Option[ChallongeTournament]] => Future[Boolean] =
        _.flatMap {
          case Some(ct) => service.saveTournament(ct.tournament)
          case None     => Future.successful(false)
        }

      val process: String => Future[Boolean] = load andThen save

      val tournaments = List(
        "DeathfateStarLeague",
        "DeathfateStarLeague2",
        "DeathfateStarLeague3",
        "DeathfateChallengerStarLeague1",
        "DeathfateChallengerStarLeague2",
        "DeathfateSuperStarLeague",
        "DeathfateSuperStarLeague2"
      )

      val resultFut = Future.traverse(tournaments)(process)
      whenReady(resultFut) { result => assertResult(7)(result.length) }
    }

  }

}
