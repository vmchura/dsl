package models.services

import java.util.UUID

import models.{Tournament, TournamentSerieID, TournamentSeries}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Span}

import scala.concurrent.ExecutionContext.Implicits.global
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

import scala.concurrent.Future
import scala.language.postfixOps

class TournamentSeriesServiceTest
    extends PlaySpec
    with GuiceOneAppPerSuite
    with ScalaFutures {
  implicit override val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = Span(100, Seconds), interval = Span(1, Seconds))

  val service: TournamentSeriesService =
    app.injector.instanceOf(classOf[TournamentSeriesService])

  val tournamentService: TournamentService =
    app.injector.instanceOf(classOf[TournamentService])

  "TournamentSeriesService" should {
    "Save and load new series" in {
      val id = TournamentSerieID(UUID.randomUUID())
      val res = for {
        _ <- service.createSeries(TournamentSeries(id, "KKCM", Nil))
        load <- service.findSeries(id)
      } yield {
        load.nonEmpty
      }
      whenReady(res) { k => assert(k) }
    }
    "Save and load new season" in {
      val id = TournamentSerieID(UUID.randomUUID())
      val res = for {
        _ <- service.createSeries(TournamentSeries(id, "KKCM", Nil))
        _ <- service.addSeason(
          id,
          Tournament(0L, "", "", "", active = true, None),
          1,
          Nil
        )
        _ <- service.addSeason(
          id,
          Tournament(1L, "", "", "", active = true, None),
          2,
          Nil
        )
        load <- service.findSeries(id)
      } yield {
        load.exists(_.seasons.length == 2)
      }
      whenReady(res) { k => assert(k) }
    }
    "Load multiple series" in {
      val ids = List.fill(4)(TournamentSerieID(UUID.randomUUID()))
      val res = for {
        _ <- Future.traverse(ids)(id =>
          service.createSeries(TournamentSeries(id, "GG1", Nil))
        )
        load <- service.allSeries()
      } yield {
        load.length >= ids.length
      }
      whenReady(res) { k => assert(k) }
    }
    "initialization tournaments" in {
      val tournaments = List(
        "DeathFate Super Star League",
        "DSL Oro",
        "DSL Plata",
        "DSL Bronce",
        "DeathFate Challenger Star League",
        "DSL"
      ).map(name =>
        TournamentSeries(TournamentSerieID(UUID.randomUUID()), name, Nil)
      )
      val result = Future.traverse(tournaments)(service.createSeries)
      whenReady(result) { k => assertResult(6)(k.length) }
    }

    def initWinners(
        seriesName: String,
        tournament: Long,
        season: Int,
        winner: String,
        second: String,
        third: String
    )(implicit
        series: Seq[TournamentSeries],
        tournaments: Seq[Tournament]
    ): Future[Boolean] = {
      val challanger = series.find(_.name.equals(seriesName)).get.id
      val res = for {
        a1 <- service.addSeason(
          challanger,
          tournaments.find(_.challongeID == tournament).get,
          season,
          List(
            (1, winner),
            (2, second),
            (3, third)
          )
        )
      } yield {
        a1
      }
      res
    }
    def addSeasonNoWinners(
        seriesName: String,
        tournament: Long,
        season: Int
    )(implicit
        series: Seq[TournamentSeries],
        tournaments: Seq[Tournament]
    ): Future[Boolean] = {
      val challanger = series.find(_.name.equals(seriesName)).get.id
      val res = for {
        a1 <- service.addSeason(
          challanger,
          tournaments.find(_.challongeID == tournament).get,
          season,
          Nil
        )
      } yield {
        a1
      }
      res
    }

    "initialization winners" in {
      implicit val series: Seq[TournamentSeries] =
        service.allSeries().futureValue
      implicit val tournaments: Seq[Tournament] =
        tournamentService.findAllTournaments().futureValue
      println(tournaments.mkString("\n"))
      println(series.mkString("\n"))

      whenReady(
        initWinners(
          "DeathFate Challenger Star League",
          8588305,
          1,
          "515016301102628867",
          "361607214156480512",
          "704783922856460288"
        )
      ) { r =>
        assert(r)
      }
      whenReady(
        initWinners(
          "DeathFate Challenger Star League",
          8757224,
          2,
          "739512036081533039",
          "704783922856460288",
          "706508203910758449"
        )
      ) { r =>
        assert(r)
      }
      whenReady(
        initWinners(
          "DeathFate Challenger Star League",
          8883318,
          3,
          "735981050349617274",
          "707668129072807996",
          "342136594469355520"
        )
      ) { r =>
        assert(r)
      }

      whenReady(
        initWinners(
          "DSL",
          8415514,
          1,
          "703923862735290419",
          "573145638221643777",
          "704767012701733054"
        )
      ) { r =>
        assert(r)
      }

      whenReady(
        initWinners(
          "DSL",
          8606853,
          2,
          "638506536078934016",
          "712393782531719168",
          "417161473110835210"
        )
      ) { r =>
        assert(r)
      }

      whenReady(
        initWinners(
          "DSL",
          8805101,
          3,
          "700212754635686018",
          "743519482236108860",
          "713872163370958938"
        )
      ) { r =>
        assert(r)
      }
      whenReady(
        initWinners(
          "DSL",
          8932418,
          4,
          "753057376579354754",
          "722863554960818286",
          "722708287476334613"
        )
      ) { r =>
        assert(r)
      }
      whenReady(
        initWinners(
          "DeathFate Super Star League",
          8606893,
          1,
          "422190200828198913",
          "696491979483381820",
          "603344544284803113"
        )
      ) { r =>
        assert(r)
      }

      whenReady(
        initWinners(
          "DeathFate Super Star League",
          8805276,
          2,
          "304663678299668482",
          "422190200828198913",
          "643521674762780713"
        )
      ) { r =>
        assert(r)
      }
      whenReady(
        initWinners(
          "DeathFate Super Star League",
          8932466,
          3,
          "603344544284803113",
          "304663678299668482",
          "429814514343477253"
        )
      ) { r =>
        assert(r)
      }

      whenReady(
        addSeasonNoWinners("DeathFate Super Star League", 9076933L, 4)
      ) { r =>
        assert(r)
      }

      whenReady(addSeasonNoWinners("DSL", 9076955L, 5)) { r =>
        assert(r)
      }

      whenReady(
        addSeasonNoWinners("DeathFate Challenger Star League", 9024519L, 4)
      ) { r =>
        assert(r)
      }
    }
  }
}
