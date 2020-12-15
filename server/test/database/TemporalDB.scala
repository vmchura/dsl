package database

import models.Tournament
import models.services.TournamentService
import org.scalatest.TestData
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Span}
import org.scalatestplus.play._
import play.api.http.MimeTypes
import play.api.test._
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.modules.reactivemongo.ReactiveMongoApi

class TemporalDB extends PlaySpec with GuiceOneAppPerTest with ScalaFutures {

  implicit override val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = Span(5, Seconds))

  override def newAppForTest(td: TestData): Application = {
    import sys.process._

    "mongo dsl-test --eval \"db.dropDatabase();\"".!

    System.setProperty("config.resource", "test-application.conf")

    GuiceApplicationBuilder()
      .configure(Map("ehcacheplugin" -> "disabled"))
      .build()

  }

  "Each test" should {
    "load config from config test ad-hoc" in {
      assert(
        app.configuration
          .getOptional[String]("mongodb.uri")
          .exists(_.endsWith("dsl-test"))
      )
    }
  }
  private def initTournament(): Boolean = {
    val tournamentService: TournamentService =
      app.injector.instanceOf(classOf[TournamentService])
    tournamentService
      .saveTournament(
        Tournament(1L, "1Str", "DServer", "TorunamentName", active = true)
      )
      .futureValue
  }

  "Tournament" should {
    "Not be inserted" in {
      val tournamentService: TournamentService =
        app.injector.instanceOf(classOf[TournamentService])
      assert(tournamentService.findAllTournaments().futureValue.toList.isEmpty)
    }
    "Be inserted" in {
      val tournamentService: TournamentService =
        app.injector.instanceOf(classOf[TournamentService])
      initTournament()
      assert(tournamentService.findAllTournaments().futureValue.toList.nonEmpty)
    }
  }

}
