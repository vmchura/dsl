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

  "TournamentSeriesService" should {
    "Save and load new series" in {
      val id = TournamentSerieID(UUID.randomUUID())
      val res = for {
        _ <-
          service.createSeries(TournamentSeries(id, "KKCM", None, "Black", Nil))
        load <- service.findSeries(id)
      } yield {
        load.nonEmpty
      }
      whenReady(res) { k => assert(k) }
    }
    "Save and load new season" in {
      val id = TournamentSerieID(UUID.randomUUID())
      val res = for {
        _ <-
          service.createSeries(TournamentSeries(id, "KKCM", None, "Black", Nil))
        _ <- service.addSeason(
          id,
          Tournament(0L, "", "", "", active = true, None),
          1
        )
        _ <- service.addSeason(
          id,
          Tournament(1L, "", "", "", active = true, None),
          2
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
          service.createSeries(TournamentSeries(id, "GG1", None, "Gree", Nil))
        )
        load <- service.allSeries()
      } yield {
        load.length >= ids.length
      }
      whenReady(res) { k => assert(k) }
    }
  }
}
