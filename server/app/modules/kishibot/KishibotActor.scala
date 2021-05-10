package modules.kishibot

import ackcord.APIMessage.{MessageCreate, MessageUpdate}
import ackcord.data.{
  Guild,
  MessageId,
  RoleId,
  TextChannelId,
  UserId,
  Message => DMessage
}
import ackcord._
import ackcord.requests.CreateMessage
import ackcord.syntax._
import ackcord.{ClientSettings, DiscordClient}
import akka.actor.typed.{Behavior, PostStop}
import akka.actor.typed.scaladsl.Behaviors
import com.google.inject.Provides
import play.api.Configuration
import play.api.libs.concurrent.ActorModule
import utils.Logger

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}
object KishibotActor extends ActorModule with Logger {
  sealed trait InternalCommand
  val allRolesRegistered = Seq(
    "BroncePlayer",
    "PlataPlayer",
    "OroPlayer",
    "DSSLPlayer",
    "RedemBroncePlayer",
    "RedemPlataPlayer",
    "RedemOroPlayer",
    "RedemDSSLPlayer"
  )
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
  sealed trait RoleChange {
    def roleID: RoleId
    def userID: UserId
  }
  case class AddRoleChange(userID: UserId, roleID: RoleId) extends RoleChange
  case class RemoveRoleChange(userID: UserId, roleID: RoleId) extends RoleChange

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

        val roleName = (if (message.content.contains("REDEMPTION")) "Redem"
                        else "") + channelRole(message.channelId)
        val otherRoleNames =
          allRolesRegistered.filterNot(_.equals(roleName)).toList
        val otherRoles =
          otherRoleNames.flatMap(rn => guild.rolesByName(rn).headOption)

        guild.rolesByName(roleName).headOption.foreach { role =>
          val postID = message.content
            .substring(message.content.indexOf(':'))
            .filter(_.isDigit)
            .mkString

          val messageInscription = discordClient.requestsHelper.run(
            ackcord.requests
              .GetChannelMessage(message.channelId, MessageId(postID))
          )

          val membersMentionedFut = messageInscription.map(m => {

            def getUsersID(prev: List[UserId], line: String, origin: Int)
                : List[UserId] = {
              val u = line.indexOf("<@!", origin)

              if (u >= 0) {
                val v = line.indexOf(">", u)

                if (v >= 0 && v - u == (3 + 18 + 1 - 1)) {
                  if (
                    v + 1 < line.length
                    /*&& (line.charAt(v + 1) == 'P' ||
                    line.charAt(v + 1) == 'T' ||
                    line.charAt(v + 1) == 'Z')*/
                  )
                    getUsersID(
                      UserId(line.substring(u + 3, v)) :: prev,
                      line,
                      v + 1
                    )
                  else
                    getUsersID(prev, line, v + 1)
                } else {
                  prev
                }
              } else {
                prev
              }
            }
            val usersID =
              getUsersID(Nil, m.content.replace(" ", "").toUpperCase, 0)

            usersID
          })

          val rolesRequest: OptFuture[List[RoleChange]] =
            membersMentionedFut.map { membersMentioned =>
              val removals = for {
                userId <- membersMentioned
                roleRemove <- otherRoles
              } yield {
                println(s"To remove $userId / ${roleRemove.name}")
                RemoveRoleChange(userId, roleRemove.id)
              }

              val insertions = membersMentioned.map(userId => {
                println(s"To add $userId / ${role.name}")
                AddRoleChange(userId, role.id)

              })
              insertions ++ removals
            }
          rolesRequest.map { x =>
            val u = x.foldLeft(
              Future.successful(Nil): Future[List[Option[String]]]
            )((prevResult, roleChange) => {
              for {
                prev <- prevResult
                guildMember <-
                  discordClient.requestsHelper
                    .run(
                      ackcord.requests
                        .GetGuildMember(guild.id, roleChange.userID)
                    )
                    .value
                /*_ <- Future {
                  print(". ")
                  Thread.sleep(1000)
                }*/
                roleInsertion <- (
                    guildMember
                      .fold(OptFuture.pure("Not GM"))(gm =>
                        (
                          roleChange,
                          gm.roles.contains(roleChange.roleID)
                        ) match {
                          case (AddRoleChange(_, _), true) =>
                            OptFuture.pure("Already has the role")
                          case (AddRoleChange(_, roleID), false) =>
                            discordClient.requestsHelper
                              .run(
                                gm.toGuildMember(guild.id)
                                  .addRole(roleID)
                              )
                              .map(_ => "Request sent")
                          case (RemoveRoleChange(_, roleID), true) =>
                            discordClient.requestsHelper
                              .run(
                                gm.toGuildMember(guild.id)
                                  .removeRole(roleID)
                              )
                              .map(_ => "Request sent")
                          case (RemoveRoleChange(_, _), false) =>
                            OptFuture.pure("Already has NOT the role")
                        }
                      )
                  )
                  .value
              } yield {

                roleInsertion :: prev
              }
            })
            u.onComplete {
              case Success(value) =>
                println(s"All requests done")
              case Failure(exception) =>
                println(
                  s"Request failed ${exception.getMessage}"
                )
            }
            u
          }

          rolesRequest.value.onComplete {
            case Success(value) => println(s"Roles requests ${value.isDefined}")
            case Failure(error) =>
              println(s"Roles requests error ${error.getMessage}")
          }

        }
        println(s"Adding $roleName")
        discordClient.requestsHelper.run(
          CreateMessage.mkContent(message.channelId, s"Adding $roleName")
        )
        println(s"Removing ${otherRoleNames.mkString(", ")}")
        discordClient.requestsHelper.run(
          CreateMessage.mkContent(
            message.channelId,
            s"Removing ${otherRoleNames.mkString(", ")}"
          )
        )
      }
    }
    def checkMessage(message: DMessage): Boolean = {

      val admingID: UserId = UserId(698648718999814165L)
      val dfID: UserId = UserId(277854224795172868L)
      channelRole.contains(message.channelId) &&
      (message.authorUserId
        .contains(admingID) || message.authorUserId.contains(dfID)) &&
      message.content.contains("ROLES")

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
      println(s"Kishibot ready")
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
