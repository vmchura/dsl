package modules.teamsystem

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import com.google.inject.Inject
import models.daos.DiscordPlayerLoggedDAO
import models.services.DiscordUserService

import scala.util.{Failure, Success}
import shared.models.{DiscordID, DiscordPlayerLogged}
class DiscordPlayerWorker @Inject() (
    discordPlayerLoggedDAO: DiscordPlayerLoggedDAO,
    discordUserService: DiscordUserService
) {
  import DiscordPlayerWorker._
  def initialBehavior(
      replyTo: Option[ActorRef[DiscordPlayerWorkerResponse]]
  ): Behavior[DiscordPlayerWorkerCommand] =
    Behaviors.setup { ctx =>
      val awaitingInfoSaving =
        Behaviors.receiveMessage[DiscordPlayerWorkerCommand] {
          case DiscordInfoSaved() =>
            replyTo.foreach(_ ! Registered())
            Behaviors.stopped
          case DiscordPlayerWorkerDAOError(error) =>
            replyTo.foreach(_ ! DiscordPlayerWorkerError(error))
            Behaviors.stopped
        }

      val awaitingInfoLoading =
        Behaviors.receiveMessage[DiscordPlayerWorkerCommand] {
          case DiscordInfoLoaded(info) =>
            ctx.pipeToSelf(discordPlayerLoggedDAO.add(info)) {
              case Success(true) => DiscordInfoSaved()
              case _ =>
                DiscordPlayerWorkerDAOError("Cant save user's discord  info")
            }
            awaitingInfoSaving
          case DiscordPlayerWorkerDAOError(error) =>
            replyTo.foreach(_ ! DiscordPlayerWorkerError(error))
            Behaviors.stopped
        }

      val awaitingIfAlreadyRegistered =
        Behaviors.receiveMessage[DiscordPlayerWorkerCommand] {
          case IsRegistered(_) =>
            replyTo.foreach(_ ! Registered())
            Behaviors.stopped
          case IsNotRegistered(discordID) =>
            ctx.pipeToSelf(discordUserService.findMember(discordID)) {
              case Success(Some(info)) => DiscordInfoLoaded(info)
              case _ =>
                DiscordPlayerWorkerDAOError("Cant load user's discord  info")
            }
            awaitingInfoLoading
          case DiscordPlayerWorkerDAOError(error) =>
            replyTo.foreach(_ ! DiscordPlayerWorkerError(error))
            Behaviors.stopped
        }

      Behaviors.receiveMessage {
        case Register(discordID) =>
          ctx.pipeToSelf(discordPlayerLoggedDAO.load(discordID)) {
            case Success(Some(_)) => IsRegistered(discordID)
            case Success(None)    => IsNotRegistered(discordID)
            case Failure(exception) =>
              DiscordPlayerWorkerDAOError(exception.getMessage)
          }
          awaitingIfAlreadyRegistered
      }
    }
}
object DiscordPlayerWorker {

  sealed trait DiscordPlayerWorkerCommand

  case class Register(discordID: DiscordID) extends DiscordPlayerWorkerCommand
  case class IsRegistered(discordID: DiscordID)
      extends DiscordPlayerWorkerCommand
  case class IsNotRegistered(discordID: DiscordID)
      extends DiscordPlayerWorkerCommand
  case class DiscordInfoLoaded(discordPlayerLogged: DiscordPlayerLogged)
      extends DiscordPlayerWorkerCommand
  case class DiscordInfoCantLoad() extends DiscordPlayerWorkerCommand
  case class DiscordPlayerWorkerDAOError(reason: String)
      extends DiscordPlayerWorkerCommand
  case class DiscordInfoSaved() extends DiscordPlayerWorkerCommand

  sealed trait DiscordPlayerWorkerResponse
  case class Registered() extends DiscordPlayerWorkerResponse
  case class DiscordPlayerWorkerError(reason: String)
      extends DiscordPlayerWorkerResponse
}
