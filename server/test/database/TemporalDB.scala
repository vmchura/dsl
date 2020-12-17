package database

import com.google.inject.AbstractModule
import com.mohiva.play.silhouette.api.Environment
import com.mohiva.play.silhouette.test.FakeEnvironment
import models.{
  ChallongeTournament,
  DiscordID,
  DiscordUser,
  ReplayRecord,
  Smurf,
  User
}
import models.services.{
  ChallongeTournamentService,
  ParseReplayFileService,
  SmurfService,
  TournamentService
}
import net.codingwell.scalaguice.ScalaModule
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Span}
import org.scalatestplus.play._
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import utils.auth.DefaultEnv
import DataBaseObjects._
import models.daos.{ReplayMatchDAO, UserSmurfDAO}
import models.services.SmurfService.SmurfAdditionResult.AdditionResult

import java.io.File
import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
trait TemporalDB
    extends PlaySpec
    with GuiceOneAppPerSuite
    with ScalaFutures
    with BeforeAndAfterEach {
  implicit val env: Environment[DefaultEnv] =
    new FakeEnvironment[DefaultEnv](Seq(first_user.loginInfo -> first_user))
  class FakeModule extends AbstractModule with ScalaModule {
    private val challongeTournamentService = new ChallongeTournamentService {
      override protected def challongeApiKey: String =
        throw new NotImplementedError("challongeApiKey is not defined")

      override def findChallongeTournament(
          discordServerID: String,
          discordChanelReplayID: Option[String]
      )(tournamentID: String): Future[Option[ChallongeTournament]] = {
        val tournament = if (tournamentID.equals(tournamentTest.urlID)) {
          Some(
            ChallongeTournament(
              tournamentTest,
              Seq(
                first_participant,
                second_participant,
                third_participant,
                fourth_participant
              ),
              Seq(first_match, second_match)
            )
          )
        } else {
          None
        }

        Future.successful(tournament)
      }
    }

    private val replayMatchDAO = new ReplayMatchDAO {
      override def add(replayRecord: ReplayRecord): Future[Boolean] =
        Future.successful(true)

      override def markAsDisabled(replayID: UUID): Future[Boolean] =
        Future.successful(true)

      override def loadAllByTournament(
          tournamentID: Long
      ): Future[Seq[ReplayRecord]] = Future.successful(Nil)

      override def loadAllByMatch(
          tournamentID: Long,
          matchID: Long
      ): Future[Seq[ReplayRecord]] = Future.successful(Nil)

      override def find(replayID: UUID): Future[Option[ReplayRecord]] =
        Future.successful(None)

      override def isNotRegistered(file: File): Future[Boolean] =
        Future.successful(true)

      override def updateLocation(
          replayID: UUID,
          cloudLocation: String
      ): Future[Boolean] = Future.successful(true)
    }

    private val parseReplayFileService = new ParseReplayFileService {
      override def parseFile(file: File): Future[Either[String, String]] = {
        import sys.process._
        val buffer = new StringBuilder()
        val res =
          s"/home/vmchura/Games/screp/cmd/screp/screp ${file.getAbsolutePath}"
            .!(ProcessLogger(line => buffer.append(line)))
        if (res == 0) {
          Future.successful(Right(buffer.toString()))
        } else {
          println(buffer.toString())
          Future.failed(
            new IllegalArgumentException("screp not installed correctly")
          )
        }

      }
    }

    override def configure(): Unit = {
      bind[Environment[DefaultEnv]].toInstance(env)
      bind[ChallongeTournamentService].toInstance(challongeTournamentService)
      bind[ReplayMatchDAO].toInstance(replayMatchDAO)
      bind[ParseReplayFileService].toInstance(parseReplayFileService)
    }
  }
  System.setProperty("config.resource", "test-application.conf")

  override lazy val app: Application = GuiceApplicationBuilder()
    .configure(Map("ehcacheplugin" -> "disabled"))
    .overrides(new FakeModule)
    .build()

  implicit override val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = Span(60, Seconds))

  override def beforeEach(): Unit = {
    import sys.process._
    "mongo dsl-test --eval \"db.dropDatabase();\"".!(ProcessLogger(_ => ()))

  }

  protected def initTournament(): Boolean = {
    val tournamentService: TournamentService =
      app.injector.instanceOf(classOf[TournamentService])
    tournamentService
      .saveTournament(database.DataBaseObjects.tournamentTest)
      .futureValue
  }
  protected def initUser(user: User): Boolean = {
    val userSmurfDAO: UserSmurfDAO =
      app.injector.instanceOf(classOf[UserSmurfDAO])
    userSmurfDAO
      .addUser(
        DiscordUser(
          user.loginInfo.providerKey,
          user.fullName.getOrElse("Anonymous"),
          None
        )
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
      val tournaments =
        tournamentService.findAllTournaments().futureValue.toList
      assert(tournaments.nonEmpty)
    }
  }

}
