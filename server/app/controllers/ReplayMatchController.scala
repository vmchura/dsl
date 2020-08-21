package controllers
import java.util.UUID

import javax.inject._
import jobs.{CannotSaveResultMatch, CannotSmurf, ReplayService}
import models.{MatchPK, MatchResult, MatchSmurf}
import models.daos.{MatchResultDAO, ReplayMatchDAO, UserSmurfDAO}
import models.services.ParseReplayFileService
import play.api.mvc._
import play.api.i18n.I18nSupport
import play.api.libs.Files
import play.api.libs.json.Json
import shared.models.ActionByReplay
import shared.models.ActionBySmurf._
import upickle.default._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}
@Singleton
class ReplayMatchController @Inject()(scc: SilhouetteControllerComponents,
                                      replayService: ReplayService,
                                      matchResultDAO: MatchResultDAO,
                                      parseFile: ParseReplayFileService,
                                      smurfDAO: UserSmurfDAO,
                                      replayMatchDAO: ReplayMatchDAO

                                     )(
                                       implicit
                                       assets: AssetsFinder,
                                       ex: ExecutionContext
                                     )extends   AbstractAuthController(scc) with I18nSupport {



  def addReplayToMatch(tournamentID: Long, matchID: Long,discordUser1: String, discordUser2: String): Action[MultipartFormData[Files.TemporaryFile]] = silhouette.SecuredAction.async(parse.multipartFormData) { implicit request =>
    import jobs.eitherError
    import jobs.flag2Future
    def secureName(fileName: String): String = fileName.filter(ch => ch.isLetterOrDigit || ch=='.' || ch == '-' || ch == '-').mkString("")
    val result = Redirect(routes.TournamentController.showMatchesToUploadReplay(tournamentID))
    def insertOnProperlySmurfList(input: Either[Option[String],Option[String]], discordUserID: String): (UUID,MatchPK) => Future[Boolean] = {
      input match {
        case Left(Some(value)) => (resultID: UUID, m: MatchPK) => smurfDAO.addSmurf(discordUserID,MatchSmurf(resultID, m,value))
        case Right(Some(value)) => (resultID: UUID, m: MatchPK) => smurfDAO.addNotCheckedSmurf(discordUserID,MatchSmurf(resultID, m,value))
        case _ => (_,_) => Future.successful(true)
      }
    }
    val outerExecution = for{
      replay_file <- request.body.file("replay_file")
      /*
      player1 <- request.body.dataParts.get("player1").flatMap(_.headOption)
      player2 <- request.body.dataParts.get("player2").flatMap(_.headOption)
      winner <- request.body.dataParts.get("winner").flatMap(_.headOption.flatMap(_.toIntOption))
      nicks <- request.body.dataParts.get("nicks").flatMap(_.headOption.flatMap(_.toIntOption))

       */
    }yield{
      val file = replay_file.ref.toFile
      val newReplayMatchID = UUID.randomUUID()
      val execution = for {
        action            <- parseFile.parseFileAndBuildAction(file, discordUser1,discordUser2)
        nicks <- action match {
          case Right(ActionByReplay(_, _, _, Correlated1d1rDefined, _)) => Future.successful(1)
          case Right(ActionByReplay(_, _, _, Correlated2d2rDefined, _)) => Future.successful(1)
          case Right(ActionByReplay(_, _, _, Correlated1d2rDefined, _)) => Future.successful(2)
          case Right(ActionByReplay(_, _, _, Correlated2d1rDefined, _)) => Future.successful(2)
          case Right(ActionByReplay(_, _, _, CorrelatedParallelDefined, _)) => Future.successful(1)
          case Right(ActionByReplay(_, _, _, CorrelatedCruzadoDefined, _)) => Future.successful(1)
          case Right(ActionByReplay(_, _, _, SmurfsEmpty, _)) => request.body.dataParts.get("nicks").flatMap(_.headOption.flatMap(_.toIntOption)) match {
            case Some(value) => Future.successful(value)
            case None => Future.failed(new IllegalArgumentException("no nicks provided when needed"))
          }
          case _ => Future.failed(new IllegalArgumentException("no nicks can be calculated"))
        }
        player1 <- action match {
          case Right(ActionByReplay(_,Some(smurf1),Some(_),_,_)) => Future.successful(smurf1)
          case _ => Future.failed(new IllegalArgumentException("no player1 provided when needed"))
        }
        player2 <- action match {
          case Right(ActionByReplay(_,Some(_),Some(smurf2),_,_)) => Future.successful(smurf2)
          case _ => Future.failed(new IllegalArgumentException("no player2 provided when needed"))
        }
        winner <- action match {
          case Right(ActionByReplay(_,Some(_),Some(_),_,win)) => Future.successful(win)
          case _ => Future.failed(new IllegalArgumentException("no winner provided when needed"))
        }
        smurfForDiscord1 <- Future.successful(action match {
          case Right(ActionByReplay(_, smurf1, _, Correlated1d1rDefined, _)) => Left(smurf1)
          case Right(ActionByReplay(_, smurf1, _, Correlated2d2rDefined, _)) => Right(smurf1)
          case Right(ActionByReplay(_, _, smurf2, Correlated1d2rDefined, _)) => Left(smurf2)
          case Right(ActionByReplay(_, _, smurf2, Correlated2d1rDefined, _)) => Right(smurf2)
          case Right(ActionByReplay(_, smurf1, _, CorrelatedParallelDefined, _)) => Left(smurf1)
          case Right(ActionByReplay(_, _, smurf2, CorrelatedCruzadoDefined, _)) => Left(smurf2)
          case Right(ActionByReplay(_, smurf1, smurf2, SmurfsEmpty, _)) => nicks match {
            case 1 => Right(smurf1)
            case 2 => Right(smurf2)
            case _ => Right(None)
          }
          case _ => Right(None)
        })


        smurfForDiscord2 <- Future.successful(action match {
          case Right(ActionByReplay(_, _, smurf2, Correlated1d1rDefined, _)) => Right(smurf2)
          case Right(ActionByReplay(_, _, smurf2, Correlated2d2rDefined, _)) => Left(smurf2)
          case Right(ActionByReplay(_, smurf1, _, Correlated1d2rDefined, _)) => Right(smurf1)
          case Right(ActionByReplay(_, smurf1, _, Correlated2d1rDefined, _)) => Left(smurf1)
          case Right(ActionByReplay(_, _, smurf2, CorrelatedParallelDefined, _)) => Left(smurf2)
          case Right(ActionByReplay(_, smurf1, _, CorrelatedCruzadoDefined, _)) => Left(smurf1)
          case Right(ActionByReplay(_, smurf1, smurf2, SmurfsEmpty, _)) => nicks match {
            case 1 => Right(smurf2)
            case 2 => Right(smurf1)
            case _ => Right(None)
          }
          case _ => Right(None)
        })
        replayPushedTry <- replayService.pushReplay(tournamentID,matchID,file, request.identity, secureName(replay_file.filename))(newReplayMatchID)
        _               <- replayPushedTry.withFailure
        resultSaved     <- matchResultDAO.save(MatchResult(newReplayMatchID,tournamentID, matchID, discordUser1, discordUser2,player1,player2, winner))
        _ <- resultSaved.withFailure(CannotSaveResultMatch)
        insertionSmurf1 <- insertOnProperlySmurfList(smurfForDiscord1,discordUser1)(newReplayMatchID,MatchPK(tournamentID,matchID))
        insertionSmurf2 <- insertOnProperlySmurfList(smurfForDiscord2,discordUser2)(newReplayMatchID,MatchPK(tournamentID,matchID))
        _ <- insertionSmurf1.withFailure(CannotSmurf)
        _ <- insertionSmurf2.withFailure(CannotSmurf)
      }yield{
        result.flashing("success" -> s"${secureName(replay_file.filename)} guardado!")
      }

      execution.transformWith{
        case Success(value) =>  Future.successful(value)
        case Failure(_) => Future.successful(result.flashing("error" -> s"${secureName(replay_file.filename)} ERROR!"))
      }
    }

    outerExecution.getOrElse(Future.successful(result.flashing("error" -> s"intentas hackearme? ERROR!")))


  }

  def parseReplay(discordUser1: String, discordUser2: String): Action[MultipartFormData[Files.TemporaryFile]] = silhouette.SecuredAction.async(parse.multipartFormData){ implicit request =>

    def buildResult(messageFut: Future[Either[String,ActionByReplay]]) = {
      messageFut.map{ message =>

        Ok(Json.obj("response" -> write(message)))
      }

    }
    val defaultValue: Future[Either[String,ActionByReplay]] = Future.successful(Left("Sin archivo enviado"))
    val result: Future[Either[String,ActionByReplay]] = request.body.file("replay_file").fold(defaultValue)(file => parseFile.parseFileAndBuildAction(file.ref.toFile, discordUser1,discordUser2))
    buildResult(result)
  }

  def downloadReplay(replayID: UUID, replayName: String): Action[AnyContent] = Action.async{ implicit request =>
    replayService.downloadReplay(replayID, replayName).map{
      case Left(error) => Redirect(routes.Application.index()).flashing("error" -> error.toString)
      case Right(file) => Ok.sendFile(file,inline = true, _ => Some(replayName))
    }

  }

  def deleteReplay(replayID: UUID): Action[AnyContent] = silhouette.SecuredAction(WithAdmin()).async{ implicit request =>
    def  resultSuccess(tournamentID: Long) = Redirect(routes.TournamentController.showMatches(tournamentID))
    val resultError = Redirect(routes.Application.index())
    for{
      replayOpt <- replayMatchDAO.find(replayID)
      result <- replayOpt match {
        case Some(replay) =>
          replayService.disableReplay(replayID).map{
            case Left(error) => resultSuccess(replay.tournamentID).flashing("error" -> error.toString)
            case Right(true) => resultSuccess(replay.tournamentID).flashing("success" -> "replay eliminado!")
            case Right(false) => resultSuccess(replay.tournamentID).flashing("error" -> "error eliminando la replay!")
          }
        case None =>
          Future.successful(resultError.flashing("error" -> "Replay corrupta en base de datos"))
      }
    }yield{
      result
    }
  }



}
