package controllers
import java.util.UUID

import javax.inject._
import jobs.{CannotSaveResultMatch, ParseFile, ReplayPusher}
import models.{DiscordUser, MatchResult, UserSmurf}
import models.daos.{MatchResultDAO, UserSmurfDAO}
import models.services.ParticipantsService
import play.api.mvc._
import play.api.i18n.I18nSupport
import play.api.libs.Files
import play.api.libs.json.Json
import shared.models.{ActionByReplay, ActionBySmurf}
import upickle.default._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}
@Singleton
class ReplayMatchController @Inject()(scc: SilhouetteControllerComponents,
                                      replayPusher: ReplayPusher,
                                      fileParser: ParseFile,
                                      matchResultDAO: MatchResultDAO,
                                      participantsService: ParticipantsService,
                                      userSmurfDAO: UserSmurfDAO
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
    implicit class eitherToFailure[A](either: Either[String,A]){
      def withFailure(): Future[A] = either match {
        case Left(error) => Future.failed(new IllegalStateException(error))
        case Right(a) => Future.successful(a)
      }
    }
    case class DiscordUserRegistered(noDiscordUserFound: Boolean, foundOnlyOne: Boolean, anyDiscordUserIfMoreFound: Option[UserSmurf], discordUserIfOnlyOneFound: Option[UserSmurf])

    def convertToChaUserRegistered(participants: List[UserSmurf]): DiscordUserRegistered = {
      participants match {
        case Nil      =>  DiscordUserRegistered(noDiscordUserFound = true,foundOnlyOne = false, None, None)
        case d :: Nil =>  DiscordUserRegistered(noDiscordUserFound = false,foundOnlyOne = true, None, Some(d))
        case d :: _ => DiscordUserRegistered(noDiscordUserFound = false,foundOnlyOne = false,Some(d),None)
      }
    }
    def keyIsSame(pk: Option[UserSmurf], test: String): Boolean = pk.map(_.discordUser.discordID).contains(test)
    import ActionBySmurf._

    val result = request.body.file("replay_file").fold(buildResult(Future.successful(Left("Sin archivo enviado")))){ replay_file =>
      val message = for{
        jsonStringEither              <- fileParser.parseFile(replay_file.ref.toFile)
        jsonString                    <- jsonStringEither.withFailure()
        replayParsedEither            <- Future.successful(fileParser.parseJsonResponse(jsonString))
        replayParsed                  <- replayParsedEither.withFailure()
        participantsWithFirstSmurf    <- userSmurfDAO.findBySmurf(replayParsed.player1)
        participantsWithSecondSmurf   <- userSmurfDAO.findBySmurf(replayParsed.player2)
      }yield{
        val withFirstSmurf = convertToChaUserRegistered(participantsWithFirstSmurf)
        val withSecondSmurf = convertToChaUserRegistered(participantsWithSecondSmurf)


        val action = (replayParsed.player1.equals(replayParsed.player2),withFirstSmurf, withSecondSmurf) match {
          case (true,_,_) => ImpossibleToDefine
          case (_,DiscordUserRegistered(true,_,_,_),DiscordUserRegistered(true,_,_,_)) => SmurfsEmpty
          case (_,DiscordUserRegistered(true,_,_,_),DiscordUserRegistered(false,true,_,pk2))  if keyIsSame(pk2,discordUser1) || keyIsSame(pk2,discordUser2)  => CompletelyDefined
          case (_,DiscordUserRegistered(false,true,_,pk1),DiscordUserRegistered(true,_,_,_))  if keyIsSame(pk1,discordUser1) || keyIsSame(pk1,discordUser2)  => CompletelyDefined
          case (_,DiscordUserRegistered(false,true,_,pk1),DiscordUserRegistered(false,true,_,pk2))  if keyIsSame(pk1,discordUser1) && keyIsSame(pk2,discordUser2) || keyIsSame(pk1,discordUser2) && keyIsSame(pk2,discordUser1)  => CompletelyDefined
          case _ => ImpossibleToDefine
        }
        ActionByReplay(defined = true,Some(replayParsed.player1), Some(replayParsed.player2), action, replayParsed.winner)
      }

      val messageWithError: Future[Either[String, ActionByReplay]] = message.map(v => Right(v)).recover(e => Left(e.toString))
      buildResult(messageWithError)
    }

    result
  }



}
