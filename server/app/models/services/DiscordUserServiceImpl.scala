package models.services
import models.DiscordUser

import scala.concurrent.Future
import sttp.client._
import sttp.client.asynchttpclient.WebSocketHandler
import sttp.client.asynchttpclient.future.AsyncHttpClientFutureBackend
import utils.Logger
import play.api.libs.json._
import javax.inject.Inject
import play.api.Configuration
import scala.concurrent.ExecutionContext.Implicits.global
class DiscordUserServiceImpl @Inject()(configuration: Configuration) extends DiscordUserService with Logger{
  implicit val sttpBackend: SttpBackend[Future, Nothing, WebSocketHandler] = AsyncHttpClientFutureBackend()
  override protected val bot_token: String = configuration.get[String]("discord.bottoken")
  override def findMembersOnGuild(guildID: String): Future[Seq[DiscordUser]] = {
    val responseFut = basicRequest.header("Authorization",s"Bot $bot_token").get(uri"https://discord.com/api/guilds/$guildID/members").send()

    responseFut.map { _.body match
      {
        case Left(errorMessage) =>
          logger.error(s"Error on findMembersOnGuild: $errorMessage")
          Nil

        case Right(body) =>
          try {
            val jsArray = Json.parse(body).as[JsArray]

            jsArray.value.map(v => {
              DiscordUser((v \ "user" \ "id").as[String], (v \ "user" \ "username").as[String])
            }).toSeq
          } catch {
            case _ : Throwable =>
              logger.error(s"Error on findMembersOnGuild: $body is not a json or an array of users")
              Nil
          }
      }
    }
  }
}
