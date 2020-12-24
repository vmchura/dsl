package controllers
import akka.actor.typed.ActorRef
import com.mohiva.play.silhouette.api.actions.SecuredRequest

import java.util.UUID
import javax.inject._
import jobs.{CannotSaveResultMatch, CannotSmurf, ReplayService}
import models.{MatchPK, MatchResult, MatchSmurf}
import models.daos.{
  MatchResultDAO,
  ReplayMatchDAO,
  TicketReplayDAO,
  UserSmurfDAO
}
import models.services.{ReplayActionBuilderService, ReplayDeleterService}
import modules.usertrajectory.GameReplayResumenManager.{
  MessageReplayResumen,
  ReplayAdded
}
import org.joda.time.DateTime
import play.api.mvc._
import play.api.i18n.I18nSupport
import play.api.libs.Files
import play.api.libs.json.Json
import shared.models.{
  ChallongeOneVsOneDefined,
  ChallongeOneVsOneMatchGameResult,
  ChallongePlayer,
  ChallongePlayerDefined,
  DiscordByHistory,
  DiscordByLogic,
  DiscordIDSource,
  DiscordIDSourceDefined
}
import shared.models.StarCraftModels.SCPlayer
import upickle.default._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}
@Singleton
class ReplayMatchController @Inject() (
    scc: SilhouetteControllerComponents,
    replayService: ReplayService,
    replayDeleterService: ReplayDeleterService,
    matchResultDAO: MatchResultDAO,
    smurfDAO: UserSmurfDAO,
    replayMatchDAO: ReplayMatchDAO,
    ticketReplayDAO: TicketReplayDAO,
    replayActionBuilderService: ReplayActionBuilderService,
    gameReplayResumenManager: ActorRef[MessageReplayResumen]
)(implicit
    assets: AssetsFinder,
    ex: ExecutionContext
) extends AbstractAuthController(scc)
    with I18nSupport {

  private def addReplayToMatchExecutor(
      tournamentID: Long,
      matchID: Long,
      discordIDFirstPlayer: String,
      discordIDSecond: String,
      result: Result,
      builderIfEmpty: (
          Map[String, Seq[String]],
          ChallongeOneVsOneMatchGameResult
      ) => Future[ChallongeOneVsOneDefined]
  )(
      request: SecuredRequest[EnvType, MultipartFormData[Files.TemporaryFile]]
  ): Future[Result] = {
    import jobs.eitherError
    import jobs.flag2Future
    import models.ReplayRecordResumen.OneVsOneDefined2ReplayRecordResumen
    def secureName(fileName: String): String =
      fileName
        .filter(ch => ch.isLetterOrDigit || ch == '.' || ch == '-' || ch == '-')
        .mkString("")
    def insertOnProperlySmurfList(
        discordID: DiscordIDSourceDefined,
        smurf: String
    ): (UUID, MatchPK) => Future[Boolean] = {
      discordID.withSource match {
        case DiscordByHistory(value) =>
          (resultID: UUID, m: MatchPK) =>
            smurfDAO.addSmurf(value, MatchSmurf(resultID, m, smurf))
        case DiscordByLogic(value) =>
          (resultID: UUID, m: MatchPK) =>
            smurfDAO.addNotCheckedSmurf(
              value,
              MatchSmurf(resultID, m, smurf)
            )
        case _ => (_, _) => Future.successful(true)
      }
    }
    if (ticketReplayDAO.ableToUpload(request.identity.userID, DateTime.now())) {

      ticketReplayDAO.uploading(request.identity.userID, DateTime.now())

      val outerExecution = for {
        replay_file <- request.body.file("replay_file")

      } yield {
        val file = replay_file.ref.toFile
        val newReplayMatchID = UUID.randomUUID()
        val execution = for {
          challongeMatchGameResult <-
            replayActionBuilderService.parseFileAndBuildAction(
              file,
              discordIDFirstPlayer,
              discordIDSecond
            )
          challongeOnveVsOneMatchGameResult <- {
            challongeMatchGameResult match {
              case Right(
                    ChallongeOneVsOneMatchGameResult(
                      ChallongePlayer(
                        Right(
                          DiscordIDSource(Right(Some(discordIDWinner)))
                        ),
                        playerWinner @ SCPlayer(_, _)
                      ),
                      ChallongePlayer(
                        Right(DiscordIDSource(Right(Some(discordIDLoser)))),
                        playerLoser @ SCPlayer(_, _)
                      )
                    )
                  ) =>
                Future.successful(
                  ChallongeOneVsOneDefined(
                    ChallongePlayerDefined(
                      DiscordIDSourceDefined.buildByHistory(
                        discordIDWinner
                      ),
                      playerWinner
                    ),
                    ChallongePlayerDefined(
                      DiscordIDSourceDefined.buildByHistory(discordIDLoser),
                      playerLoser
                    )
                  )
                )
              case Right(
                    ChallongeOneVsOneMatchGameResult(
                      ChallongePlayer(
                        Right(
                          DiscordIDSource(Right(Some(discordIDWinner)))
                        ),
                        playerWinner @ SCPlayer(_, _)
                      ),
                      ChallongePlayer(
                        Right(DiscordIDSource(Left(Some(discordIDLoser)))),
                        playerLoser @ SCPlayer(_, _)
                      )
                    )
                  ) =>
                Future.successful(
                  ChallongeOneVsOneDefined(
                    ChallongePlayerDefined(
                      DiscordIDSourceDefined.buildByHistory(
                        discordIDWinner
                      ),
                      playerWinner
                    ),
                    ChallongePlayerDefined(
                      DiscordIDSourceDefined.buildByLogic(discordIDLoser),
                      playerLoser
                    )
                  )
                )
              case Right(
                    ChallongeOneVsOneMatchGameResult(
                      ChallongePlayer(
                        Right(
                          DiscordIDSource(Left(Some(discordIDWinner)))
                        ),
                        playerWinner @ SCPlayer(_, _)
                      ),
                      ChallongePlayer(
                        Right(DiscordIDSource(Right(Some(discordIDLoser)))),
                        playerLoser @ SCPlayer(_, _)
                      )
                    )
                  ) =>
                Future.successful(
                  ChallongeOneVsOneDefined(
                    ChallongePlayerDefined(
                      DiscordIDSourceDefined.buildByLogic(
                        discordIDWinner
                      ),
                      playerWinner
                    ),
                    ChallongePlayerDefined(
                      DiscordIDSourceDefined.buildByHistory(discordIDLoser),
                      playerLoser
                    )
                  )
                )
              case Right(
                    challongeGameResult @ ChallongeOneVsOneMatchGameResult(
                      ChallongePlayer(
                        Right(DiscordIDSource(Right(None))),
                        _
                      ),
                      ChallongePlayer(
                        Right(DiscordIDSource(Right(None))),
                        _
                      )
                    )
                  ) =>
                builderIfEmpty(request.body.dataParts, challongeGameResult)

              case Left(error) =>
                Future.failed(
                  new IllegalArgumentException(error)
                )
              case _ =>
                Future.failed(
                  new IllegalArgumentException("Cant parse Correctly")
                )

            }
          }

          replayPushedTry <- replayService.pushReplay(
            tournamentID,
            matchID,
            file,
            request.identity,
            secureName(replay_file.filename)
          )(newReplayMatchID)
          _ <- replayPushedTry.withFailure
          resultSaved <- matchResultDAO.save(
            MatchResult(
              newReplayMatchID,
              tournamentID,
              matchID,
              challongeOnveVsOneMatchGameResult.winner.discordID.discordIDValue,
              challongeOnveVsOneMatchGameResult.loser.discordID.discordIDValue,
              challongeOnveVsOneMatchGameResult.winner.player.smurf,
              challongeOnveVsOneMatchGameResult.loser.player.smurf,
              1
            )
          )
          _ <- resultSaved.withFailure(CannotSaveResultMatch)
          insertionSmurf1 <- insertOnProperlySmurfList(
            challongeOnveVsOneMatchGameResult.winner.discordID,
            challongeOnveVsOneMatchGameResult.winner.player.smurf
          )(newReplayMatchID, MatchPK(tournamentID, matchID))
          insertionSmurf2 <- insertOnProperlySmurfList(
            challongeOnveVsOneMatchGameResult.loser.discordID,
            challongeOnveVsOneMatchGameResult.loser.player.smurf
          )(newReplayMatchID, MatchPK(tournamentID, matchID))
          _ <- insertionSmurf1.withFailure(CannotSmurf)
          _ <- insertionSmurf2.withFailure(CannotSmurf)
          _ <- Future.successful(
            gameReplayResumenManager ! ReplayAdded(
              challongeOnveVsOneMatchGameResult
                .toRecordResumen(newReplayMatchID)
            )
          )
        } yield {
          result.flashing(
            "success" -> s"${secureName(replay_file.filename)} guardado!"
          )
        }

        execution.transformWith {
          case Success(value) => Future.successful(value)
          case Failure(error) =>
            logger.error(error.toString)
            Future.successful(
              result.flashing(
                "error" -> s"${secureName(replay_file.filename)} ERROR!"
              )
            )
        }
      }

      outerExecution.getOrElse(
        Future.successful(
          result.flashing("error" -> s"intentas hackearme? ERROR!")
        )
      )
    } else {
      Future.successful(
        result.flashing(
          "error" -> s"we need more energy, procesando anterior replay, espera un poco y vuelve a intentarlo"
        )
      )
    }
  }

  def addReplayToMatch(
      tournamentID: Long,
      matchID: Long,
      discordIDQuerying: String,
      discordIDRival: String
  ): Action[MultipartFormData[Files.TemporaryFile]] =
    silhouette.SecuredAction.async(parse.multipartFormData) {
      implicit request =>
        val result = Redirect(
          routes.TournamentController.showMatchesToUploadReplay(tournamentID)
        )
        def buildFromMySmurf(
            dataParts: Map[String, Seq[String]],
            gameResult: ChallongeOneVsOneMatchGameResult
        ): Future[ChallongeOneVsOneDefined] = {
          try {
            val Some(Seq(smurfQuerying)) = dataParts.get("mySmurf")

            val playerWinner = gameResult.winner.player
            val playerLoser = gameResult.loser.player
            val winnerSmurf = playerWinner.smurf
            val loserSmurf = playerLoser.smurf
            def buildGameDefined(
                disordWinnerID: String,
                discordLoserID: String
            ) =
              ChallongeOneVsOneDefined(
                winner = ChallongePlayerDefined(
                  DiscordIDSourceDefined.buildByLogic(disordWinnerID),
                  playerWinner
                ),
                loser = ChallongePlayerDefined(
                  DiscordIDSourceDefined.buildByLogic(discordLoserID),
                  playerLoser
                )
              )
            if (winnerSmurf.equals(smurfQuerying)) {
              Future.successful(
                buildGameDefined(discordIDQuerying, discordIDRival)
              )
            } else {
              if (loserSmurf.equals(smurfQuerying)) {
                Future.successful(
                  buildGameDefined(discordIDRival, discordIDQuerying)
                )
              } else {
                Future.failed(
                  new IllegalArgumentException(
                    "Cant parse Correctly, manual data incoherent"
                  )
                )

              }

            }
          } catch {
            case _: Throwable =>
              Future.failed(
                new IllegalArgumentException(
                  "Cant parse Correctly, not manual data given"
                )
              )
          }
        }
        addReplayToMatchExecutor(
          tournamentID,
          matchID,
          discordIDQuerying,
          discordIDRival,
          result,
          buildFromMySmurf
        )(request)

    }

  def addReplayToMatchByAdmin(
      tournamentID: Long,
      matchID: Long,
      discordIDFirst: String,
      discordIDSecond: String
  ): Action[MultipartFormData[Files.TemporaryFile]] =
    silhouette.SecuredAction(WithAdmin()).async(parse.multipartFormData) {
      implicit request =>
        val result = Redirect(
          routes.TournamentController.showMatches(tournamentID)
        )
        def buildFromBothSmurfs(
            dataParts: Map[String, Seq[String]],
            gameResult: ChallongeOneVsOneMatchGameResult
        ): Future[ChallongeOneVsOneDefined] = {
          def constructIDS(): Option[List[String]] =
            (0 to 3)
              .map(i =>
                dataParts.get(s"bothIDsSmurfs[$i]").flatMap(_.headOption)
              )
              .foldLeft(Option(List.empty[String])) {
                case (prevSeq, itemOption) =>
                  for {
                    seq <- prevSeq
                    item <- itemOption
                  } yield {
                    item :: seq
                  }
              }
              .map(_.reverse)

          val (smurfFirst, smurfSecond) = constructIDS() match {
            case Some(
                  List(
                    `discordIDFirst`,
                    smurfFirst,
                    `discordIDSecond`,
                    smurfSecond
                  )
                ) =>
              (smurfFirst, smurfSecond)
            case Some(
                  List(
                    `discordIDSecond`,
                    smurfSecond,
                    `discordIDFirst`,
                    smurfFirst
                  )
                ) =>
              (smurfFirst, smurfSecond)
            case _ =>
              new IllegalArgumentException("No smurfs provided nor valid ones")
          }

          val playerWinner = gameResult.winner.player
          val playerLoser = gameResult.loser.player
          val winnerSmurf = playerWinner.smurf
          val loserSmurf = playerLoser.smurf
          def buildGameDefined(
              disordWinnerID: String,
              discordLoserID: String
          ) =
            ChallongeOneVsOneDefined(
              winner = ChallongePlayerDefined(
                DiscordIDSourceDefined.buildByLogic(disordWinnerID),
                playerWinner
              ),
              loser = ChallongePlayerDefined(
                DiscordIDSourceDefined.buildByLogic(discordLoserID),
                playerLoser
              )
            )
          if (
            winnerSmurf.equals(smurfFirst) && loserSmurf.equals(smurfSecond)
          ) {
            Future.successful(
              buildGameDefined(discordIDFirst, discordIDSecond)
            )
          } else {
            if (
              winnerSmurf.equals(smurfSecond) && loserSmurf.equals(smurfFirst)
            ) {
              Future.successful(
                buildGameDefined(discordIDSecond, discordIDFirst)
              )
            } else {
              Future.failed(
                new IllegalArgumentException(
                  "Cant parse Correctly, manual data incoherent"
                )
              )

            }

          }

        }

        addReplayToMatchExecutor(
          tournamentID,
          matchID,
          discordIDFirst,
          discordIDSecond,
          result,
          buildFromBothSmurfs
        )(request)
    }
  def parseReplay(
      discordUser1: String,
      discordUser2: String
  ): Action[MultipartFormData[Files.TemporaryFile]] =
    silhouette.SecuredAction.async(parse.multipartFormData) {
      implicit request =>
        def buildResult(
            messageFut: Future[Either[String, ChallongeOneVsOneMatchGameResult]]
        ) = {
          messageFut.map { message =>
            Ok(Json.obj("response" -> write(message)))
          }

        }
        val defaultValue
            : Future[Either[String, ChallongeOneVsOneMatchGameResult]] =
          Future.successful(Left("Sin archivo enviado"))
        val result: Future[Either[String, ChallongeOneVsOneMatchGameResult]] =
          request.body
            .file("replay_file")
            .fold(defaultValue)(file =>
              replayActionBuilderService.parseFileAndBuildAction(
                file.ref.toFile,
                discordUser1,
                discordUser2
              )
            )
        buildResult(result)
    }

  def downloadReplay(replayID: UUID, replayName: String): Action[AnyContent] =
    Action.async { implicit request =>
      replayService.downloadReplay(replayID, replayName).map {
        case Left(error) =>
          Redirect(routes.Application.index())
            .flashing("error" -> error.toString)
        case Right(file) =>
          Ok.sendFile(file, inline = true, _ => Some(replayName))
      }

    }

  def deleteReplay(replayID: UUID): Action[AnyContent] =
    silhouette.SecuredAction(WithAdmin()).async { implicit request =>
      def resultSuccess(tournamentID: Long) =
        Redirect(routes.TournamentController.showMatches(tournamentID))
      val resultError = Redirect(routes.Application.index())
      for {
        replayOpt <- replayMatchDAO.find(replayID)
        result <- replayOpt match {
          case Some(replay) =>
            replayDeleterService.disableReplay(replayID).map {
              case Left(error) =>
                resultSuccess(replay.tournamentID)
                  .flashing("error" -> error.toString)
              case Right(true) =>
                resultSuccess(replay.tournamentID)
                  .flashing("success" -> "replay eliminado!")
              case Right(false) =>
                resultSuccess(replay.tournamentID)
                  .flashing("error" -> "error eliminando la replay!")
            }
          case None =>
            Future.successful(
              resultError
                .flashing("error" -> "Replay corrupta en base de datos")
            )
        }
      } yield {
        result
      }
    }

}
