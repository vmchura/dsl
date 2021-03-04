package modules.teamsystem

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import com.google.inject.Provides
import models.daos.ReplayMatchDAO
import models.daos.teamsystem.TeamMetaReplayTeamDAO
import play.api.libs.concurrent.ActorModule

import scala.concurrent.Future
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global
object UniqueReplayWatcher extends ActorModule {
  sealed trait PrivateCommand

  sealed trait Command extends PrivateCommand
  override type Message = Command
  case class Pending(hash: String, replyTo: Option[ActorRef[Enqueued]])
      extends Command
  case class Unique(hash: String, replyTo: ActorRef[UniqueResponse])
      extends Command
  case class RemovePending(hash: String, replyTo: Option[ActorRef[Removed]])
      extends Command

  case class NotRegisteredOnDB(replyTo: ActorRef[UniqueResponse])
      extends Command
  case class RegisteredOnDB(replyTo: ActorRef[UniqueResponse]) extends Command
  case class ErrorGettingDB(reason: String, replyTo: ActorRef[UniqueResponse])
      extends Command

  sealed trait Response
  sealed trait UniqueResponse extends Response
  case class IsUnique() extends UniqueResponse
  case class IsNotUnique() extends UniqueResponse
  case class IsPending() extends UniqueResponse
  case class ErrorResponse(reason: String) extends UniqueResponse
  case class Removed() extends Response
  case class Enqueued() extends Response
  @Provides
  def apply(
      replayTeamDAO: TeamMetaReplayTeamDAO,
      replayMatchDAO: ReplayMatchDAO
  ): Behavior[Command] = {

    def isRegistered(hash: String): Future[Boolean] =
      replayTeamDAO
        .isRegistered(hash)
        .zip(replayMatchDAO.isRegistered(hash))
        .map {
          case (teams, dsl) => teams || dsl
        }

    Behaviors.setup { ctx =>
      var pending: Set[String] = Set.empty
      Behaviors
        .receiveMessage[PrivateCommand] {
          case Pending(hash, replyTo) =>
            pending = pending + hash
            replyTo.foreach(_ ! Enqueued())
            Behaviors.same
          case RemovePending(hash, replyTo) =>
            pending = pending - hash
            replyTo.foreach(_ ! Removed())
            Behaviors.same
          case Unique(hash, replyTo) =>
            if (pending.contains(hash)) {
              replyTo ! IsPending()
            } else {
              ctx.pipeToSelf(isRegistered(hash)) {
                case Success(true)  => RegisteredOnDB(replyTo)
                case Success(false) => NotRegisteredOnDB(replyTo)
                case Failure(exception) =>
                  ErrorGettingDB(exception.getMessage, replyTo)
              }
            }
            Behaviors.same
          case RegisteredOnDB(replyTo) =>
            replyTo ! IsNotUnique()
            Behaviors.same
          case NotRegisteredOnDB(replyTo) =>
            replyTo ! IsUnique()
            Behaviors.same
          case ErrorGettingDB(reason, replyTo) =>
            replyTo ! ErrorResponse(reason)
            Behaviors.same
        }
        .narrow
    }
  }
}
