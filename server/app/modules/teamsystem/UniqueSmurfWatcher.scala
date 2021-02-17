package modules.teamsystem

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import com.google.inject.Provides
import models.Smurf
import models.daos.{DiscordPlayerLoggedDAO, ValidUserSmurfDAO}
import play.api.libs.concurrent.ActorModule
import shared.models.{DiscordID, DiscordPlayerLogged}

import scala.util.{Failure, Success}

object UniqueSmurfWatcher extends ActorModule {
  sealed trait InternalCommand
  sealed trait Command extends InternalCommand

  override type Message = Command

  case class LocateOwner(smurf: Smurf, replyTo: ActorRef[Response])
      extends Command
  private case class DiscordIDSmurf(
      discordID: DiscordID,
      replyTo: ActorRef[Response]
  ) extends InternalCommand
  private case class DiscordLoggedSmurf(
      discordPlayerLogged: DiscordPlayerLogged,
      replyTo: ActorRef[UserOwner]
  ) extends InternalCommand
  private case class SmurfNotAssignedOnDB(replyTo: ActorRef[SmurfNotAssigned])
      extends InternalCommand
  private case class UniqueSmurfWatcherError(
      reason: String,
      replyTo: ActorRef[ErrorSmurfWatcher]
  ) extends InternalCommand

  sealed trait Response
  case class UserOwner(discordPlayerLogged: DiscordPlayerLogged)
      extends Response
  case class SmurfNotAssigned() extends Response
  case class ErrorSmurfWatcher(reason: String) extends Response

  @Provides
  def apply(
      validUserSmurfDAO: ValidUserSmurfDAO,
      discordPlayerLoggedDAO: DiscordPlayerLoggedDAO
  ): Behavior[Command] =
    Behaviors
      .setup[InternalCommand] { ctx =>
        Behaviors.receiveMessage {
          case LocateOwner(smurf, replyTo) =>
            ctx.pipeToSelf(validUserSmurfDAO.findOwner(smurf)) {
              case Success(Some(discordID)) =>
                DiscordIDSmurf(discordID, replyTo)
              case Success(None) => SmurfNotAssignedOnDB(replyTo)
              case Failure(exception) =>
                UniqueSmurfWatcherError(exception.getMessage, replyTo)
            }
            Behaviors.same
          case DiscordIDSmurf(discordID, replyTo) =>
            ctx.pipeToSelf(discordPlayerLoggedDAO.load(discordID)) {
              case Success(Some(userLogged)) =>
                DiscordLoggedSmurf(userLogged, replyTo)
              case Success(None) =>
                UniqueSmurfWatcherError("User name not found", replyTo)
              case Failure(exception) =>
                UniqueSmurfWatcherError(exception.getMessage, replyTo)
            }
            Behaviors.same

          case SmurfNotAssignedOnDB(replyTo) =>
            replyTo ! SmurfNotAssigned()
            Behaviors.same

          case UniqueSmurfWatcherError(reason, replyTo) =>
            replyTo ! ErrorSmurfWatcher(reason)
            Behaviors.same
          case DiscordLoggedSmurf(userLogged, replyTo) =>
            replyTo ! UserOwner(userLogged)
            Behaviors.same
        }

      }
      .narrow
}
