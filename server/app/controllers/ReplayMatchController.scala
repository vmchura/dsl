package controllers
import java.util.UUID

import javax.inject._
import jobs.{CannotSaveResultMatch, ParseFile, ReplayPusher}
import models.MatchResult
import models.daos.MatchResultDAO
import play.api.mvc._
import play.api.i18n.I18nSupport
import play.api.libs.Files
import play.api.libs.json.Json
import shared.models.ActionByReplay
import upickle.default._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}
@Singleton
class ReplayMatchController @Inject()(scc: SilhouetteControllerComponents,
                                      replayPusher: ReplayPusher,
                                      matchResultDAO: MatchResultDAO,
                                      parseFile: ParseFile

                                     )(
                                       implicit
                                       assets: AssetsFinder,
                                       ex: ExecutionContext
                                     )extends   AbstractAuthController(scc) with I18nSupport {



  def addReplayToMatch(tournamentID: Long, matchID: Long): Action[MultipartFormData[Files.TemporaryFile]] = silhouette.SecuredAction.async(parse.multipartFormData) { implicit request =>
    import jobs.eitherError
    import jobs.flag2Future
    val result = Redirect(routes.TournamentController.showMatchesToUploadReplay(tournamentID))

    val outerExecution = for{
      replay_file <- request.body.file("replay_file")
      player1 <- request.body.dataParts.get("player1").flatMap(_.headOption)
      player2 <- request.body.dataParts.get("player2").flatMap(_.headOption)
      player1Discord <- request.body.dataParts.get("player1Discord").flatMap(_.headOption)
      player2Discord <- request.body.dataParts.get("player2Discord").flatMap(_.headOption)
      winner <- request.body.dataParts.get("winner").flatMap(_.headOption.flatMap(_.toIntOption))
      nicks <- request.body.dataParts.get("nicks").flatMap(_.headOption.flatMap(_.toIntOption))
      nicksSame <- nicks match {
        case 1 => Some(true)
        case 2 => Some(false)
        case _ => None
      }
    }yield{

      val execution = for {
        replayPushedTry <- replayPusher.pushReplay(tournamentID,matchID,replay_file.ref.toFile, request.identity, replay_file.filename)
        _ <- replayPushedTry.withFailure
        resultSaved <- matchResultDAO.save(MatchResult(UUID.randomUUID(),tournamentID, matchID, player1Discord, player2Discord,player1,player2, winner,uploadedOnChallonge = false,nicksSame))
        _ <- resultSaved.withFailure(CannotSaveResultMatch)
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
