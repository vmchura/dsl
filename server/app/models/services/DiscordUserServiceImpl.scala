package models.services
import models.{DiscordDiscriminator, DiscordID, DiscordUser, DiscordUserData, GuildID}

import scala.concurrent.Future
import sttp.client._
import sttp.client.asynchttpclient.WebSocketHandler
import sttp.client.asynchttpclient.future.AsyncHttpClientFutureBackend
import utils.Logger
import play.api.libs.json._
import javax.inject.Inject
import play.api.Configuration
import eu.timepit.refined.api.RefType

import scala.concurrent.ExecutionContext.Implicits.global
class DiscordUserServiceImpl @Inject()(configuration: Configuration) extends DiscordUserService with Logger{
  implicit val sttpBackend: SttpBackend[Future, Nothing, WebSocketHandler] = AsyncHttpClientFutureBackend()
  override protected val bot_token: String = configuration.get[String]("discord.bottoken")

  private def parseDiscordUser(jsValue: JsValue): Option[DiscordUser] = {
    try{
      Some(DiscordUser((jsValue \ "user" \ "id").as[String],
        (jsValue \ "nick").as[String],
        (jsValue \ "user" \ "discriminator").asOpt[String]))
    }catch{
      case _: Throwable => None
    }
  }
  private def parseDiscordUserData(jsValue: JsValue): Option[DiscordUserData] = {
    try{
      val userName = (jsValue \ "nick").as[String]
      val discordID = DiscordID((jsValue \ "user" \ "id").as[String])
      val discriminator: Either[String, DiscordDiscriminator] = RefType.applyRef[DiscordDiscriminator]((jsValue \ "user" \ "discriminator").as[String])
      val avatarURL = (jsValue \ "user"\ "avatar").asOpt[String]

      discriminator match {
        case Right(discriminatorValue) => Some(DiscordUserData(discordID, userName, discriminatorValue,avatarURL))
        case Left(_) => None
      }
    }catch{
      case _: Throwable => None
    }
  }

  private def findDiscordUsers[T](guildID: GuildID, parser: JsValue => Option[T]): Future[Option[Seq[T]]] = {
    val responseFut = basicRequest.header("Authorization",s"Bot $bot_token").get(uri"https://discord.com/api/guilds/${guildID.id}/members?limit=1000").send()

    responseFut.map { _.body match
    {
      case Left(errorMessage) =>
        logger.error(s"Error on findMembersOnGuild: $errorMessage")
        None

      case Right(body) =>
        try {
          val jsArray = Json.parse(body).as[JsArray]
          Some(jsArray.value.flatMap(v => {
            parser(v)
          }).toSeq)
        } catch {
          case _ : Throwable =>
            logger.error(s"Error on findMembersOnGuild: $body is not a json or an array of users")
            None
        }
    }
    }
  }
  override def findMembersOnGuild(guildID: String): Future[Option[Seq[DiscordUser]]] = {
    findDiscordUsers(GuildID(guildID),parseDiscordUser)
  }

  override def findMembersOnGuildData(guildID: GuildID): Future[Option[Seq[DiscordUserData]]] = {
    findDiscordUsers(guildID,parseDiscordUserData)
  }
}
