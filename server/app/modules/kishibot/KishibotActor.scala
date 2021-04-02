package modules.kishibot

import ackcord.APIMessage.{MessageCreate, MessageUpdate}
import ackcord.data.{Guild, TextChannelId, UserId, Message => DMessage}
import ackcord._
import akka.NotUsed
import ackcord.commands._
import ackcord.syntax._
import akka.NotUsed
import ackcord.{APIMessage, CacheState, ClientSettings, DiscordClient}
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import com.google.inject.Provides
import play.api.Configuration
import play.api.libs.concurrent.ActorModule
import utils.Logger

import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global
object KishibotActor extends ActorModule with Logger {
  sealed trait InternalCommand
  override type Message = InternalCommand
  case class ClientCreated(discordClient: DiscordClient) extends InternalCommand
  case class ClientCreationFailed(reason: String) extends InternalCommand
  def withClient(discordClient: DiscordClient): Behavior[InternalCommand] = {

    Behaviors.same
  }

  private def addEventsHandlers(discordClient: DiscordClient): Unit = {

    def handleMessage(guild: Guild, message: DMessage)(implicit
        c: CacheSnapshot
    ): Unit = {
      if (checkMessage(message)) {
        message.mentions.foreach { userId =>
          for {
            m <- guild.memberById(userId)
            role <- guild.rolesByName("Protoss").headOption
          } yield {
            discordClient.requestsHelper.run(m.addRole(role.id)).map(_ => ())
          }
        }
      }
    }
    def checkMessage(message: DMessage): Boolean = {

      val channelViewing: TextChannelId = TextChannelId(736009485310754898L)
      val admingID: UserId = UserId(698648718999814165L)
      logger.logger.debug(s"${message.channelId} - $channelViewing")
      logger.logger.debug(s"${message.authorUserId} - ${Some(admingID)}")
      val a = message.channelId == channelViewing
      val b = message.authorUserId == Some(admingID)
      val c = message.content.contains("HOla")
      logger.logger.debug(s"$a - $b - $c")
      a && b && c
    }

    discordClient.onEventSideEffects { implicit c =>
      {

        case MessageCreate(
              guild: Option[Guild],
              message: DMessage,
              _
            ) => {
          guild.fold(logger.logger.error("Not guild provided"))(
            handleMessage(_, message)
          )

        }
        case MessageUpdate(
              guild: Option[Guild],
              message: DMessage,
              _
            ) =>
          guild.fold(logger.logger.error("Not guild provided"))(
            handleMessage(_, message)
          )

      }
    }
  }

  @Provides
  def apply(configuration: Configuration): Behavior[InternalCommand] =
    Behaviors.setup { ctx =>
      val token: String = configuration.get[String]("kishibot.token")
      val clientSettings = ClientSettings(token)
      ctx.pipeToSelf(clientSettings.createClient()) {
        case Success(value)     => ClientCreated(value)
        case Failure(exception) => ClientCreationFailed(exception.getMessage)
      }

      Behaviors.receiveMessage {
        case ClientCreated(discordClient) =>
          logger.logger.info("discord client created")
          addEventsHandlers(discordClient)
          withClient(discordClient)
        case ClientCreationFailed(reason) =>
          logger.logger.error(s"Cant create discrod client $reason")
          Behaviors.stopped
      }
    }
}
