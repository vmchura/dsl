package modules.teamsystem

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import com.google.inject.Provides
import models.teamsystem.{RequestJoin, RequestJoinID, TeamID}
import play.api.libs.concurrent.ActorModule
import shared.models.DiscordID

import scala.concurrent.duration.DurationInt
import scala.language.{existentials, postfixOps}

object RequestJoinManager extends ActorModule {

  sealed trait InternalCommand

  case class RequestSaved() extends InternalCommand

  case class MetaValid() extends InternalCommand
  case class MetaInvalid() extends InternalCommand
  case class ReqRemoved() extends InternalCommand
  case class MemberAdded() extends InternalCommand
  case class RequestValid() extends InternalCommand
  case class RequestLoaded(request: RequestJoin) extends InternalCommand
  case class RequestInvalid() extends InternalCommand

  case class RequestError(reason: String) extends InternalCommand

  case class WorkerDone(worker: ActorRef[Command]) extends InternalCommand
  case class WorkerTimeOut(worker: ActorRef[Command]) extends InternalCommand

  sealed trait Command extends InternalCommand

  case class RequestJoinCommand(
      from: DiscordID,
      teamID: TeamID,
      replyTo: ActorRef[RequestJoinResponse]
  ) extends Command
  case class RemoveRequest(
      requestID: RequestJoinID,
      replyTo: Option[ActorRef[RemoveRequestResponse]]
  ) extends Command

  case class AcceptRequest(
      requestID: RequestJoinID,
      replyTo: ActorRef[AcceptRequestResponse]
  ) extends Command

  sealed trait Response
  sealed trait RequestJoinResponse extends Response
  case class RequestSuccessful() extends RequestJoinResponse

  sealed trait RemoveRequestResponse extends Response
  case class RequestRemovedSuccessful() extends RemoveRequestResponse

  sealed trait AcceptRequestResponse extends Response
  case class RequestAcceptedSuccessful() extends AcceptRequestResponse

  case class RequestProcessError(reason: String)
      extends RequestJoinResponse
      with RemoveRequestResponse
      with AcceptRequestResponse

  override type Message = InternalCommand

  @Provides
  def apply(
      requestJoinWorker: RequestJoinWorker
  ): Behavior[InternalCommand] = {

    Behaviors.setup[InternalCommand] { ctx =>
      var workers = Set.empty[ActorRef[Command]]
      Behaviors.receiveMessage {
        case WorkerDone(worker) =>
          if (workers.contains(worker)) {
            workers = workers - worker
          }
          Behaviors.same
        case WorkerTimeOut(worker) =>
          if (workers.contains(worker)) {
            ctx.stop(worker)
            workers = workers - worker
          }
          Behaviors.same
        case command if command.isInstanceOf[Command] =>
          val worker = ctx.spawnAnonymous(requestJoinWorker.initialBehavior())
          workers = workers + worker
          ctx.scheduleOnce(10 seconds, ctx.self, WorkerTimeOut(worker))
          ctx.watchWith(worker, WorkerDone(worker))
          worker ! command.asInstanceOf[Command]
          Behaviors.same
        case _ => Behaviors.unhandled
      }
    }
  }

}
