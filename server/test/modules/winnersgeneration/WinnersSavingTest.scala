package modules.winnersgeneration

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import com.google.inject.AbstractModule
import models.daos.UserGuildDAO
import models.services.{TournamentSeriesService, TournamentService}
import models.{
  DiscordID,
  Tournament,
  TournamentSeason,
  TournamentSerieID,
  TournamentSeries,
  UserGuild
}
import modules.winnersgeneration.WinnersGathering.{
  Gather,
  GatheringFail,
  GatheringSucess,
  WinnersGatheringResponse
}
import modules.winnersgeneration.WinnersSaving.{
  SaveWinners,
  WinnersSavedSuccessfully,
  WinnersSavingFailed,
  WinnersSavingResponse
}
import net.codingwell.scalaguice.ScalaModule
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder

import java.util.UUID
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
import scala.concurrent.ExecutionContext.Implicits.global
class WinnersSavingTest
    extends ScalaTestWithActorTestKit
    with AnyWordSpecLike
    with GuiceOneAppPerSuite
    with ScalaFutures {

  class FakeModule extends AbstractModule with ScalaModule {

    val tournamentSeriesService = new TournamentSeriesService {
      var tournamentSeries: Seq[TournamentSeries] = Seq.empty
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
        Future.successful(tournamentSeries)

      override def findSeries(
          id: TournamentSerieID
      ): Future[Option[TournamentSeries]] = ???

      override def addSeason(
          id: TournamentSerieID,
          tournamentID: Long,
          season: Int,
          winners: List[(Int, String)]
      ): Future[Boolean] =
        Future.successful(true).map { flag =>
          {
            val tournamentSeason =
              TournamentSeason(tournamentID, season = season, winners = winners)
            val index = tournamentSeries.indexWhere(_.id == id)
            if (index >= 0) {
              val series = tournamentSeries(index)
              tournamentSeries.updated(
                index,
                series.copy(seasons = tournamentSeason +: series.seasons)
              )
            } else {
              tournamentSeries = TournamentSeries(
                id,
                "",
                Seq(tournamentSeason)
              ) +: tournamentSeries

            }

            flag
          }
        }
    }

    override def configure(): Unit = {
      bind[TournamentSeriesService].toInstance(tournamentSeriesService)
    }
  }

  System.setProperty("config.resource", "test-application.conf")

  override lazy val app: Application = GuiceApplicationBuilder()
    .configure(Map("ehcacheplugin" -> "disabled"))
    .overrides(new FakeModule)
    .build()

  "Winner Saving" must {
    "Save information" in {
      val tss = app.injector.instanceOf(classOf[TournamentSeriesService])
      val winnersGathering = testKit.spawn(WinnersSaving(tss))
      val replyTo =
        testKit.createTestProbe[WinnersSavingResponse](
          s"probe-response-saving"
        )

      val tsID = TournamentSerieID(UUID.randomUUID())
      val players: List[(Int, DiscordID)] =
        List((1, DiscordID("a")), (2, DiscordID("b")))
      winnersGathering ! SaveWinners(
        WinnersInformation(0L, tsID, players, 4),
        replyTo.ref
      )
      replyTo.expectMessage(WinnersSavedSuccessfully())
      whenReady(tss.allSeries()) { series =>
        assertResult(
          Seq(
            TournamentSeries(
              tsID,
              "",
              Seq(
                TournamentSeason(
                  0L,
                  4,
                  players.map { case (pos, id) => (pos, id.id) }
                )
              )
            )
          )
        )(series)
      }
    }
    "Fail saving information" in {
      val tss = new TournamentSeriesService {
        override def createSeries(
            series: TournamentSeries
        ): Future[TournamentSeries] = ???

        override def addSeason(
            id: TournamentSerieID,
            tournamentID: Long,
            season: Int,
            winners: List[(Int, String)]
        ): Future[Boolean] = Future.failed(new NotImplementedError())

        override def allSeries(): Future[Seq[TournamentSeries]] =
          Future.successful(Nil)

        override def findSeries(
            id: TournamentSerieID
        ): Future[Option[TournamentSeries]] = ???
      }
      val winnersGathering = testKit.spawn(WinnersSaving(tss))
      val replyTo =
        testKit.createTestProbe[WinnersSavingResponse](
          s"probe-response-saving"
        )

      val tsID = TournamentSerieID(UUID.randomUUID())
      val players: List[(Int, DiscordID)] =
        List((1, DiscordID("a")), (2, DiscordID("b")))
      winnersGathering ! SaveWinners(
        WinnersInformation(0L, tsID, players, 4),
        replyTo.ref
      )
      replyTo.expectMessage(WinnersSavingFailed())

    }
    "Fail after no saving information before 10 seconds" in {
      val tss = new TournamentSeriesService {
        override def createSeries(
            series: TournamentSeries
        ): Future[TournamentSeries] = ???

        override def addSeason(
            id: TournamentSerieID,
            tournamentID: Long,
            season: Int,
            winners: List[(Int, String)]
        ): Future[Boolean] = Future.failed(new NotImplementedError())

        override def allSeries(): Future[Seq[TournamentSeries]] =
          akka.pattern.after(12 seconds)(Future.successful(Nil))

        override def findSeries(
            id: TournamentSerieID
        ): Future[Option[TournamentSeries]] = ???
      }
      val winnersGathering = testKit.spawn(WinnersSaving(tss))
      val replyTo =
        testKit.createTestProbe[WinnersSavingResponse](
          s"probe-response-saving"
        )

      val tsID = TournamentSerieID(UUID.randomUUID())
      val players: List[(Int, DiscordID)] =
        List((1, DiscordID("a")), (2, DiscordID("b")))
      winnersGathering ! SaveWinners(
        WinnersInformation(0L, tsID, players, 4),
        replyTo.ref
      )
      replyTo.expectMessage(15 seconds, WinnersSavingFailed())

    }
  }

}
