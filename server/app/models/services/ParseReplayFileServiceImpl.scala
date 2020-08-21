package models.services

import java.io.{File, FileInputStream}
import java.util.Base64

import javax.inject.Inject
import models.UserSmurf
import models.daos.UserSmurfDAO
import play.api.Configuration
import play.api.libs.json.{JsArray, JsString, Json}
import shared.models.{ActionByReplay, ActionBySmurf, ReplayDescriptionShared}
import sttp.client._
import sttp.client.asynchttpclient.WebSocketHandler
import sttp.client.asynchttpclient.future.AsyncHttpClientFutureBackend

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ParseReplayFileServiceImpl @Inject()(configuration: Configuration, userSmurfDAO: UserSmurfDAO) extends ParseReplayFileService{
  private val lambda_x_api_key = configuration.get[String]("lambda.apikey")

  def parseFile(file: File): Future[Either[String,String]] = {

    val in = new FileInputStream(file)
    val bytes = new Array[Byte](file.length.toInt)
    in.read(bytes)
    in.close()
    val encoded = Base64.getEncoder.encodeToString(bytes)

    // the `query` parameter is automatically url-encoded
    // `sort` is removed, as the value is not defined
    val request = basicRequest.
      header("x-api-key",lambda_x_api_key).
      body(Json.obj("replayfile" -> JsString(encoded), "filename" -> JsString("este es un archivo X")).toString()).post(uri"https://o1hyykheh4.execute-api.us-east-2.amazonaws.com/default/replayParser")


    implicit val backend: SttpBackend[Future, Nothing, WebSocketHandler] = AsyncHttpClientFutureBackend()



    // alternatively, if you prefer to pass the backend explicitly, instead
    // of using implicits, you can also call:
    val response = backend.send(request)

    response.map(_.body)


  }

  def parseFileAndBuildAction(file: File, discordUserID1: String, discordUserID2: String): Future[Either[String,ActionByReplay]] = {
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

    val result = {
      val message = for{
        jsonStringEither              <- parseFile(file)
        jsonString                    <- jsonStringEither.withFailure()
        replayParsedEither            <- Future.successful(parseJsonResponse(jsonString))
        replayParsed                  <- replayParsedEither.withFailure()
        participantsWithFirstSmurf    <- userSmurfDAO.findBySmurf(replayParsed.player1)
        participantsWithSecondSmurf   <- userSmurfDAO.findBySmurf(replayParsed.player2)
      }yield{
        val withFirstSmurf = convertToChaUserRegistered(participantsWithFirstSmurf)
        val withSecondSmurf = convertToChaUserRegistered(participantsWithSecondSmurf)


        val action = (replayParsed.player1.equals(replayParsed.player2),withFirstSmurf, withSecondSmurf) match {
          case (true,_,_) => ImpossibleToDefine
          case (_,DiscordUserRegistered(true,_,_,_),DiscordUserRegistered(true,_,_,_)) => SmurfsEmpty
          case (_,DiscordUserRegistered(true,_,_,_),DiscordUserRegistered(false,true,_,pk2))  if keyIsSame(pk2,discordUserID1) => Correlated1d2rDefined
          case (_,DiscordUserRegistered(true,_,_,_),DiscordUserRegistered(false,true,_,pk2))  if keyIsSame(pk2,discordUserID2) => Correlated2d2rDefined
          case (_,DiscordUserRegistered(false,true,_,pk1),DiscordUserRegistered(true,_,_,_))  if keyIsSame(pk1,discordUserID1) => Correlated1d1rDefined
          case (_,DiscordUserRegistered(false,true,_,pk1),DiscordUserRegistered(true,_,_,_))  if keyIsSame(pk1,discordUserID2) => Correlated2d1rDefined
          case (_,DiscordUserRegistered(false,true,_,pk1),DiscordUserRegistered(false,true,_,pk2))  if keyIsSame(pk1,discordUserID1) && keyIsSame(pk2,discordUserID2)  => CorrelatedParallelDefined
          case (_,DiscordUserRegistered(false,true,_,pk1),DiscordUserRegistered(false,true,_,pk2))  if keyIsSame(pk1,discordUserID2) && keyIsSame(pk2,discordUserID1)  => CorrelatedCruzadoDefined
          case _ => ImpossibleToDefine
        }
        ActionByReplay(defined = true,Some(replayParsed.player1), Some(replayParsed.player2), action, replayParsed.winner)
      }

      message.map(v => Right(v)).recover(e => Left(e.toString))

    }
    result
  }
}
