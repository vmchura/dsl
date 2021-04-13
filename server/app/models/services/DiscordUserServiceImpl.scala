package models.services
import models.{DiscordUser, DiscordUserData, GuildID}

import scala.concurrent.Future
import sttp.client._
import sttp.client.asynchttpclient.WebSocketHandler
import sttp.client.asynchttpclient.future.AsyncHttpClientFutureBackend
import utils.Logger
import play.api.libs.json._

import javax.inject.Inject
import play.api.Configuration
import eu.timepit.refined.api.RefType
import shared.models.{DiscordDiscriminator, DiscordID, DiscordPlayerLogged}

import scala.concurrent.ExecutionContext.Implicits.global
class DiscordUserServiceImpl @Inject() (configuration: Configuration)
    extends DiscordUserService
    with Logger {
  implicit val sttpBackend: SttpBackend[Future, Nothing, WebSocketHandler] =
    AsyncHttpClientFutureBackend()
  override protected val bot_token: String =
    configuration.get[String]("discord.bottoken")

  private def parseDiscordUser(jsValue: JsValue): Option[DiscordUser] = {
    try {
      Some(
        DiscordUser(
          (jsValue \ "user" \ "id").as[String],
          (jsValue \ "nick")
            .asOpt[String]
            .getOrElse((jsValue \ "user" \ "username").as[String]),
          (jsValue \ "user" \ "discriminator").asOpt[String]
        )
      )
    } catch {
      case _: Throwable => None
    }
  }
  private def parseDiscordUserData(
      jsValue: JsValue
  ): Option[DiscordUserData] = {
    try {
      val userName = (jsValue \ "nick")
        .asOpt[String]
        .getOrElse((jsValue \ "user" \ "username").as[String])
      val discordID = DiscordID((jsValue \ "user" \ "id").as[String])
      val discriminator: Either[String, DiscordDiscriminator] =
        RefType.applyRef[DiscordDiscriminator](
          (jsValue \ "user" \ "discriminator").as[String]
        )
      val avatarURL = (jsValue \ "user" \ "avatar").asOpt[String]
      discriminator match {
        case Right(discriminatorValue) =>
          Some(
            DiscordUserData(discordID, userName, discriminatorValue, avatarURL)
          )
        case Left(_) => None
      }
    } catch {
      case _: Throwable => None

    }
  }

  private def findDiscordUsers[T](
      guildID: GuildID,
      parser: JsValue => Option[T]
  ): Future[Option[Seq[T]]] = {
    val responseFut = basicRequest
      .header("Authorization", s"Bot $bot_token")
      .get(uri"https://discord.com/api/guilds/${guildID.id}/members?limit=1000")
      .send()

    responseFut.map {
      _.body match {
        case Left(errorMessage) =>
          logger.error(s"Error on findMembersOnGuild: $errorMessage")
          None

        case Right(body) =>
          try {
            val jsArray = Json.parse(body).as[JsArray]
            Some(
              jsArray.value
                .flatMap(v => {
                  parser(v)
                })
                .toSeq
            )
          } catch {
            case _: Throwable =>
              logger.error(
                s"Error on findMembersOnGuild: $body is not a json or an array of users"
              )
              None
          }
      }
    }
  }
  override def findMembersOnGuild(
      guildID: String
  ): Future[Option[Seq[DiscordUser]]] = {
    findDiscordUsers(GuildID(guildID), parseDiscordUser)
  }

  override def findMembersOnGuildData(
      guildID: GuildID
  ): Future[Option[Seq[DiscordUserData]]] = {
    findDiscordUsers(guildID, parseDiscordUserData)
  }

  override def findMember(
      discordID: DiscordID
  ): Future[Option[DiscordPlayerLogged]] = {
    val responseFut = basicRequest
      .header("Authorization", s"Bot $bot_token")
      .get(uri"https://discord.com/api/users/${discordID.id}")
      .send()

    responseFut.map {
      _.body match {
        case Left(errorMessage) =>
          logger.error(s"Error on extracting member: $errorMessage")
          None
        case Right(body) =>
          try {
            val userJson = Json.parse(body)
            val discriminatorEither: Either[String, DiscordDiscriminator] =
              RefType.applyRef[DiscordDiscriminator](
                (userJson \ "discriminator").as[String]
              )

            (
              (userJson \ "bot").asOpt[Boolean],
              (userJson \ "system").asOpt[Boolean],
              discriminatorEither
            ) match {
              case (
                    None | Some(false),
                    None | Some(false),
                    Right(discriminator)
                  ) =>
                Some(
                  DiscordPlayerLogged(
                    discordID,
                    (userJson \ "username").as[String],
                    discriminator,
                    (userJson \ "avatar").asOpt[String]
                  )
                )
              case _ => None
            }

          } catch {
            case _: Throwable =>
              logger.error(
                s"Error on finding user: $body is not a json or an array of users"
              )
              None
          }
      }
    }
  }

  override def findMembersOnPost(
      channelID: String,
      messageID: String
  ): Future[Option[Seq[DiscordUser]]] = {
    val responseFut = basicRequest
      .header("Authorization", s"Bot $bot_token")
      .get(
        uri"https://discord.com/api/channels/$channelID/messages/$messageID"
      )
      .send()
    responseFut.map {
      _.body match {
        case Left(errorMessage) =>
          logger.error(s"Error on findMembersOnPost: $errorMessage")
          None

        case Right(body) =>
          try {
            val mentions = (Json.parse(body) \ "mentions").as[JsArray]

            Some(
              mentions.value.map { m =>
                DiscordUser(
                  (m \ "id").as[JsString].value,
                  (m \ "username").as[JsString].value,
                  (m \ "discriminator").asOpt[JsString].map(_.value)
                )
              }.toSeq
            )
          } catch {
            case _: Throwable =>
              logger.error(
                s"Error on findMembersOnPost: $body is not a json of message"
              )
              None
          }
      }
    }
  }
}
