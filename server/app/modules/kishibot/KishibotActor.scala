package modules.kishibot

import ackcord.APIMessage.{MessageCreate, MessageUpdate}
import ackcord.data.{Guild, TextChannelId, UserId, Message => DMessage}
import ackcord._
import ackcord.requests.{AddGuildMemberRole, RemoveGuildMemberRole}
import ackcord.syntax._
import ackcord.{ClientSettings, DiscordClient}
import akka.actor.typed.{Behavior, PostStop}
import akka.actor.typed.scaladsl.Behaviors
import com.google.inject.Provides
import play.api.Configuration
import play.api.libs.concurrent.ActorModule
import utils.Logger

import scala.util.{Failure, Success}
object KishibotActor extends ActorModule with Logger {
  sealed trait InternalCommand
  val channelRole: Map[TextChannelId, String] = Map(
    TextChannelId(703361614107246632L) -> "BroncePlayer",
    TextChannelId(785594196336574494L) -> "PlataPlayer",
    TextChannelId(703362067234553877L) -> "OroPlayer",
    TextChannelId(722170993371775067L) -> "DSSLPlayer"
  )
  /*
  val channelRole: Map[TextChannelId, String] = Map(
    TextChannelId(748452513182646295L) -> "Terran",
    TextChannelId(736015708701589556L) -> "Zerg",
    TextChannelId(736009485310754898L) -> "Protoss"
  )*/
  override type Message = InternalCommand
  case class ClientCreated(discordClient: DiscordClient) extends InternalCommand
  case class ClientCreationFailed(reason: String) extends InternalCommand
  case class EchoKishi(message: String) extends InternalCommand
  def withClient(discordClient: DiscordClient): Behavior[InternalCommand] = {

    Behaviors
      .receiveMessagePartial[InternalCommand] {
        case EchoKishi(_) =>
          Behaviors.same
      }
      .receiveSignal {
        case (_, PostStop) =>
          discordClient.logout()
          Behaviors.same
      }
  }

  private def addEventsHandlers(discordClient: DiscordClient): Unit = {

    def handleMessage(guild: Guild, message: DMessage)(implicit
        c: CacheSnapshot
    ): Unit = {
      if (checkMessage(message)) {
        val roleName = channelRole(message.channelId)
        val otherRoleNames =
          channelRole.values.filterNot(_.equals(roleName)).toList
        val otherRoles =
          otherRoleNames.flatMap(rn => guild.rolesByName(rn).headOption)

        guild.rolesByName(roleName).headOption.foreach { role =>
          val membersMentioned = message.mentions
            .filter(u => message.content.contains(u.toString))

          for {
            userId <- membersMentioned
            roleRemove <- otherRoles
          } yield {
            discordClient.requestsHelper
              .run(RemoveGuildMemberRole(guild.id, userId, roleRemove.id))
          }

          membersMentioned.foreach(userId => {
            discordClient.requestsHelper
              .run(AddGuildMemberRole(guild.id, userId, role.id))
          })

        }
      }
    }
    def checkMessage(message: DMessage): Boolean = {

      val admingID: UserId = UserId(698648718999814165L)
      val dfID: UserId = UserId(277854224795172868L)
      channelRole.contains(message.channelId) &&
      (message.authorUserId
        .contains(admingID) || message.authorUserId.contains(dfID)) &&
      message.content.contains("SOLO PARA APUNTARSE AL TORNEO")

    }

    discordClient.onEventSideEffects { implicit c =>
      {

        case MessageCreate(
              guild: Option[Guild],
              message: DMessage,
              _
            ) =>
          guild.fold(logger.logger.error("Not guild provided"))(
            handleMessage(_, message)
          )
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

      Behaviors.receiveMessagePartial {
        case ClientCreated(discordClient) =>
          logger.logger.info("discord client created")
          addEventsHandlers(discordClient)
          discordClient.login()
          withClient(discordClient)
        case ClientCreationFailed(reason) =>
          logger.logger.error(s"Cant create discrod client $reason")
          Behaviors.stopped
      }
    }
}
