package models.daos.usertrajectory

import com.google.inject.AbstractModule
import database.EmptyDBBeforeEach
import models.{DiscordPlayer, ReplayRecordResumen}
import models.daos.{MatchResultDAO, ReplayMatchDAO}
import models.services.{
  ParseReplayFileService,
  ReplayActionBuilderService,
  TestServices,
  TournamentService
}
import net.codingwell.scalaguice.ScalaModule
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Minutes, Seconds, Span}
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import shared.models.{ChallongeOneVsOneMatchGameResult, ChallongePlayer}

import java.io.File
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Try

/**
  * load from replay match, only available ones
  *
  */
class UploadResumenDataHistory
    extends PlaySpec
    with GuiceOneAppPerSuite
    with ScalaFutures
    with EmptyDBBeforeEach {
  class FakeModule extends AbstractModule with ScalaModule {
    override def configure(): Unit = {
      bind[ParseReplayFileService].toInstance(
        TestServices.parseReplayFileService
      )
    }
  }
  override lazy val app: Application = GuiceApplicationBuilder()
    .configure(Map("ehcacheplugin" -> "disabled"))
    .overrides(new FakeModule)
    .build()

  private val tournamentService =
    app.injector.instanceOf(classOf[TournamentService])
  private val replayMatchDAO = app.injector.instanceOf(classOf[ReplayMatchDAO])
  private val matchResultDAO = app.injector.instanceOf(classOf[MatchResultDAO])
  private val replayRecordResumenDAO =
    app.injector.instanceOf(classOf[ReplayRecordResumenDAO])
  private val replayActionBuilderService =
    app.injector.instanceOf(classOf[ReplayActionBuilderService])
  implicit override val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = Span(15, Minutes), interval = Span(10, Seconds))
  "UploadResumenDataHistory" must {
    "Load many replays" in {
      val data = for {
        tournaments <- tournamentService.findAllTournaments()
        replayMatches <-
          Future
            .traverse(tournaments.map(_.challongeID))(
              replayMatchDAO.loadAllByTournament
            )
            .map(_.flatten)
        matchResults <-
          Future
            .traverse(replayMatches)(rep =>
              matchResultDAO.find(rep.replayID).map(_.map(res => (res, rep)))
            )
            .map(_.flatten)
      } yield {
        matchResults.map {
          case (result, replay) => {
            val file = new File(
              s"/home/vmchura/Dropbox/Aplicaciones/dsl-replays${replay.matchName}"
            )
            if (file.exists() && replay.enabled) {

              val recordResumen = replayActionBuilderService
                .parseFileAndBuildAction(
                  file,
                  result.firstDiscordPlayer,
                  result.secondDiscordPlayer,
                  checkFileDuplicity = false
                )
                .map {
                  case Right(
                        ChallongeOneVsOneMatchGameResult(
                          ChallongePlayer(Right(winnerID), winnerPlayer),
                          ChallongePlayer(Right(loserID), loserPlayer)
                        )
                      ) =>
                    Try(
                      ReplayRecordResumen(
                        replay.replayID,
                        DiscordPlayer(
                          Some(
                            winnerID.withSource.buildDefined().discordIDValue
                          ),
                          winnerPlayer
                        ),
                        DiscordPlayer(
                          Some(
                            loserID.withSource.buildDefined().discordIDValue
                          ),
                          loserPlayer
                        ),
                        enabled = true
                      )
                    ).toOption
                  case x => {
                    println(x)
                    println(file.getAbsolutePath)
                    None
                  }
                }
              (for {
                resumen <- recordResumen
                _ <- Future.successful(resumen)
                insertion <- resumen.fold(Future.successful(1))(toAdd =>
                  replayRecordResumenDAO.add(toAdd).map(_ => 0)
                )
              } yield {
                insertion
              }).futureValue

            } else {
              1
            }
          }
        }

      }
      whenReady(data) { d => println(d.sum) }
    }
  }
}
