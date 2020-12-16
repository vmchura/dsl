package database

import com.google.inject.AbstractModule
import com.mohiva.play.silhouette.api.Environment
import com.mohiva.play.silhouette.test.FakeEnvironment
import models.{DiscordID, Smurf, Tournament, User}
import models.services.{SmurfService, TournamentService}
import net.codingwell.scalaguice.ScalaModule
import org.scalatest.{BeforeAndAfterEach, TestData}
import org.scalatestplus.play.guice.{GuiceOneAppPerSuite, GuiceOneAppPerTest}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Span}
import org.scalatestplus.play._
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import utils.auth.DefaultEnv
import DataBaseObjects._
import models.services.SmurfService.SmurfAdditionResult.AdditionResult

import scala.concurrent.ExecutionContext.Implicits.global
trait TemporalDB
    extends PlaySpec
    with GuiceOneAppPerSuite
    with ScalaFutures
    with BeforeAndAfterEach {
  implicit val env: Environment[DefaultEnv] =
    new FakeEnvironment[DefaultEnv](Seq(first_user.loginInfo -> first_user))
  class FakeModule extends AbstractModule with ScalaModule {
    override def configure(): Unit = {
      bind[Environment[DefaultEnv]].toInstance(env)
    }
  }
  System.setProperty("config.resource", "test-application.conf")

  override lazy val app: Application = GuiceApplicationBuilder()
    .configure(Map("ehcacheplugin" -> "disabled"))
    .overrides(new FakeModule)
    .build()

  implicit override val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = Span(5, Seconds))

  override def beforeEach(): Unit = {
    import sys.process._
    "mongo dsl-test --eval \"db.dropDatabase();\"".!(ProcessLogger(_ => ()))

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

  protected def addSmurfToUser(user: User, smurf: Smurf): AdditionResult = {
    val smurfService: SmurfService =
      app.injector.instanceOf(classOf[SmurfService])
    smurfService
      .addSmurf(DiscordID(user.loginInfo.providerKey), smurf)
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
