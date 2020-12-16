package database

import com.google.inject.AbstractModule
import com.mohiva.play.silhouette.api.Environment
import com.mohiva.play.silhouette.test.FakeEnvironment
import models.Tournament
import models.services.TournamentService
import net.codingwell.scalaguice.ScalaModule
import org.scalatest.TestData
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Span}
import org.scalatestplus.play._
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import utils.auth.DefaultEnv
import DataBaseObjects._
import scala.concurrent.ExecutionContext.Implicits.global
trait TemporalDB extends PlaySpec with GuiceOneAppPerTest with ScalaFutures {
  implicit val env: Environment[DefaultEnv] =
    new FakeEnvironment[DefaultEnv](Seq(first_user.loginInfo -> first_user))
  class FakeModule extends AbstractModule with ScalaModule {
    override def configure(): Unit = {
      bind[Environment[DefaultEnv]].toInstance(env)
    }
  }
  implicit override val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = Span(5, Seconds))

  override def newAppForTest(td: TestData): Application = {
    import sys.process._

    "mongo dsl-test --eval \"db.dropDatabase();\"".!

    System.setProperty("config.resource", "test-application.conf")

    GuiceApplicationBuilder()
      .configure(Map("ehcacheplugin" -> "disabled"))
      .overrides(new FakeModule)
      .build()

  }

  protected def initTournament(): Boolean = {
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
