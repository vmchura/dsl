package controllers
import java.util.UUID

import javax.inject._
import jobs.{CannotSaveResultMatch, CannotSmurf, ParseFile, ReplayPusher}
import models.{MatchPK, MatchResult, MatchSmurf}
import models.daos.{MatchResultDAO, UserSmurfDAO}
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
                                      replayPusher: ReplayPusher,
                                      matchResultDAO: MatchResultDAO,
                                      parseFile: ParseFile,
                                      smurfDAO: UserSmurfDAO

                                     )(
                                       implicit
                                       assets: AssetsFinder,
                                       ex: ExecutionContext
                                     )extends   AbstractAuthController(scc) with I18nSupport {



  def addReplayToMatch(tournamentID: Long, matchID: Long,discordUser1: String, discordUser2: String): Action[MultipartFormData[Files.TemporaryFile]] = silhouette.SecuredAction.async(parse.multipartFormData) { implicit request =>
    import jobs.eitherError
    import jobs.flag2Future
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
      player1 <- request.body.dataParts.get("player1").flatMap(_.headOption)
      player2 <- request.body.dataParts.get("player2").flatMap(_.headOption)
      winner <- request.body.dataParts.get("winner").flatMap(_.headOption.flatMap(_.toIntOption))
      nicks <- request.body.dataParts.get("nicks").flatMap(_.headOption.flatMap(_.toIntOption))
    }yield{
      val file = replay_file.ref.toFile
      val newMatchResultID = UUID.randomUUID()
      val execution = for {
        action            <- parseFile.parseFileAndBuildAction(file, discordUser1,discordUser2)

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
        replayPushedTry <- replayPusher.pushReplay(tournamentID,matchID,file, request.identity, replay_file.filename)
        _               <- replayPushedTry.withFailure
        resultSaved     <- matchResultDAO.save(MatchResult(newMatchResultID,tournamentID, matchID, discordUser1, discordUser2,player1,player2, winner))
        _ <- resultSaved.withFailure(CannotSaveResultMatch)
        insertionSmurf1 <- insertOnProperlySmurfList(smurfForDiscord1,discordUser1)(newMatchResultID,MatchPK(tournamentID,matchID))
        insertionSmurf2 <- insertOnProperlySmurfList(smurfForDiscord2,discordUser2)(newMatchResultID,MatchPK(tournamentID,matchID))
        _ <- insertionSmurf1.withFailure(CannotSmurf)
        _ <- insertionSmurf2.withFailure(CannotSmurf)
      }yield{
        result.flashing("success" -> s"${replay_file.filename} guardado!")
      }

      execution.transformWith{
        case Success(value) =>  Future.successful(value)
        case Failure(_) => Future.successful(result.flashing("error" -> s"${replay_file.filename} ERROR!"))
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



}
