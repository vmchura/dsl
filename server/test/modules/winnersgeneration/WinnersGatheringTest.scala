package modules.winnersgeneration

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import com.google.inject.AbstractModule
import models.daos.UserGuildDAO
import models.{Tournament, TournamentSerieID, TournamentSeries, UserGuild}
import models.services.{TournamentSeriesService, TournamentService}
import modules.winnersgeneration.WinnersGathering.{
  Gather,
  GatheringFail,
  GatheringSucess,
  WinnersGatheringResponse
}
import net.codingwell.scalaguice.ScalaModule
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import shared.models.DiscordID

import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
import java.util.UUID
import scala.concurrent.Future
class WinnersGatheringTest
    extends ScalaTestWithActorTestKit
    with AnyWordSpecLike
    with GuiceOneAppPerSuite {
  class FakeModule extends AbstractModule with ScalaModule {
    val tournamentService = new TournamentService {
      override def findAllTournaments(): Future[Seq[Tournament]] =
        Future.successful(Nil)

      override def findAllActiveTournaments(): Future[Seq[Tournament]] = ???

      override def saveTournament(tournament: Tournament): Future[Boolean] = ???

      override def loadTournament(
          challongeID: Long
      ): Future[Option[Tournament]] = ???

      override def findAllTournamentsByPlayer(
          userID: UUID
      ): Future[Seq[Tournament]] = ???

      override def findAllTournamentsByPlayer(
          challongeID: String
      ): Future[Seq[Tournament]] = ???

      override def findAllActiveTournamentsByPlayer(
          userID: UUID
      ): Future[Seq[Tournament]] = ???

      override def dropTournament(challongeID: Long): Future[Boolean] = ???

      override def findTournament(
          challongeID: Long
      ): Future[Option[Tournament]] = ???

      override def findAllActiveTournamentsByPlayer(
          challongeID: String
      ): Future[Seq[Tournament]] = ???
    }
    val tournamentSeriesService = new TournamentSeriesService {
      override def createSeries(
          series: TournamentSeries
      ): Future[TournamentSeries] = ???

      override def addSeason(
          id: TournamentSerieID,
          tournament: Tournament,
          season: Int,
          winners: List[(Int, String)]
      ): Future[Boolean] = ???

      override def allSeries(): Future[Seq[TournamentSeries]] =
        Future.successful(Nil)

      override def findSeries(
          id: TournamentSerieID
      ): Future[Option[TournamentSeries]] = ???

      override def addSeason(
          id: TournamentSerieID,
          tournamentID: Long,
          season: Int,
          winners: List[(Int, String)]
      ): Future[Boolean] = ???
    }
    val userGuildDAO = new UserGuildDAO {
      override def load(
          discordID: DiscordID
      ): Future[Set[models.GuildID]] = ???

      override def addGuildToUser(
          discordID: DiscordID,
          guildID: models.GuildID
      ): Future[Boolean] = ???

      override def all(): Future[Seq[UserGuild]] = Future.successful(Nil)

      override def guilds(): Future[Seq[models.GuildID]] = ???
    }
    override def configure(): Unit = {
      bind[TournamentService].toInstance(tournamentService)
      bind[TournamentSeriesService].toInstance(tournamentSeriesService)
      bind[UserGuildDAO].toInstance(userGuildDAO)
    }
  }
  System.setProperty("config.resource", "test-application.conf")

  override lazy val app: Application = GuiceApplicationBuilder()
    .configure(Map("ehcacheplugin" -> "disabled"))
    .overrides(new FakeModule)
    .build()

  "Winner Gathering" must {
    "Load information" in {
      val ts = app.injector.instanceOf(classOf[TournamentService])
      val tss = app.injector.instanceOf(classOf[TournamentSeriesService])
      val udao = app.injector.instanceOf(classOf[UserGuildDAO])
      val winnersGathering = testKit.spawn(WinnersGathering(ts, tss, udao))
      val replyTo =
        testKit.createTestProbe[WinnersGatheringResponse](
          s"probe-response-gathered"
        )

      winnersGathering ! Gather(replyTo.ref)
      replyTo.expectMessage(GatheringSucess(GatheredInformation(Nil, Nil, Nil)))
    }
    "Fail gathering information if service fails" in {
      val ts = new TournamentService {
        override def findAllTournaments(): Future[Seq[Tournament]] =
          Future.failed(new IllegalAccessError(""))

        override def findAllActiveTournaments(): Future[Seq[Tournament]] = ???

        override def saveTournament(tournament: Tournament): Future[Boolean] =
          ???

        override def loadTournament(
            challongeID: Long
        ): Future[Option[Tournament]] = ???

        override def findAllTournamentsByPlayer(
            userID: UUID
        ): Future[Seq[Tournament]] = ???

        override def findAllTournamentsByPlayer(
            challongeID: String
        ): Future[Seq[Tournament]] = ???

        override def findAllActiveTournamentsByPlayer(
            userID: UUID
        ): Future[Seq[Tournament]] = ???

        override def dropTournament(challongeID: Long): Future[Boolean] = ???

        override def findTournament(
            challongeID: Long
        ): Future[Option[Tournament]] = ???

        override def findAllActiveTournamentsByPlayer(
            challongeID: String
        ): Future[Seq[Tournament]] = ???
      }
      val tss = app.injector.instanceOf(classOf[TournamentSeriesService])
      val udao = app.injector.instanceOf(classOf[UserGuildDAO])
      val winnersGathering = testKit.spawn(WinnersGathering(ts, tss, udao))
      val replyTo =
        testKit.createTestProbe[WinnersGatheringResponse](
          s"probe-response-gathered"
        )

      winnersGathering ! Gather(replyTo.ref)
      replyTo.expectMessage(GatheringFail())
    }
    "Fail gathering information if in 10 seconds cant gather information" in {
      val ts = new TournamentService {
        override def findAllTournaments(): Future[Seq[Tournament]] =
          akka.pattern.after(12 seconds)(Future.successful(Nil))

        override def findAllActiveTournaments(): Future[Seq[Tournament]] = ???

        override def saveTournament(tournament: Tournament): Future[Boolean] =
          ???

        override def loadTournament(
            challongeID: Long
        ): Future[Option[Tournament]] = ???

        override def findAllTournamentsByPlayer(
            userID: UUID
        ): Future[Seq[Tournament]] = ???

        override def findAllTournamentsByPlayer(
            challongeID: String
        ): Future[Seq[Tournament]] = ???

        override def findAllActiveTournamentsByPlayer(
            userID: UUID
        ): Future[Seq[Tournament]] = ???

        override def dropTournament(challongeID: Long): Future[Boolean] = ???

        override def findTournament(
            challongeID: Long
        ): Future[Option[Tournament]] = ???

        override def findAllActiveTournamentsByPlayer(
            challongeID: String
        ): Future[Seq[Tournament]] = ???
      }
      val tss = app.injector.instanceOf(classOf[TournamentSeriesService])
      val udao = app.injector.instanceOf(classOf[UserGuildDAO])
      val winnersGathering = testKit.spawn(WinnersGathering(ts, tss, udao))
      val replyTo =
        testKit.createTestProbe[WinnersGatheringResponse](
          s"probe-response-gathered"
        )

      winnersGathering ! Gather(replyTo.ref)
      replyTo.expectMessage(12 seconds, GatheringFail())
    }
  }

}
