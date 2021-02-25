package modules.teamsystem

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import com.google.inject.Provides
import play.api.libs.concurrent.ActorModule
import shared.models.ReplayTeamID

import java.io.File
import scala.util.{Failure, Success}

object FilePusherActor extends ActorModule {
  sealed trait InternalCommand
  sealed trait Command extends InternalCommand
  case class Push(
      file: File,
      replayTeamID: ReplayTeamID,
      replyTo: ActorRef[Response]
  ) extends Command
  case class PushedCorrectly(replyTo: ActorRef[Response])
      extends InternalCommand
  case class PushErrorInternal(reason: String, replyTo: ActorRef[Response])
      extends InternalCommand

  sealed trait Response
  case class Pushed() extends Response
  case class PushError(reason: String) extends Response

  override type Message = Command
  @Provides
  def apply(fileSaver: FileSaver): Behavior[Command] =
    Behaviors
      .setup[InternalCommand] { ctx =>
        Behaviors.receiveMessage {
          case Push(file, replayTeamID, replyTo) =>
            ctx.pipeToSelf(fileSaver.push(file, replayTeamID.id.toString)) {
              case Success(true) => PushedCorrectly(replyTo)
              case Success(false) =>
                PushErrorInternal("Can't save file", replyTo)
              case Failure(error) =>
                PushErrorInternal(error.getMessage, replyTo)
            }
            Behaviors.same
          case PushedCorrectly(replyTo) =>
            replyTo ! Pushed()
            Behaviors.same
          case PushErrorInternal(reason, replyTo) =>
            replyTo ! PushError(reason)
            Behaviors.same
        }
      }
      .narrow
}
