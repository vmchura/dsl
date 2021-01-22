package modules.teamsystem

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import com.google.inject.Inject
import models.daos.teamsystem.InvitationDAO
import models.teamsystem.{Invitation, InvitationID}

import scala.util.{Failure, Success}

class InvitationDAOWorker @Inject() (invitationDAO: InvitationDAO) {
  import InvitationDAOWorker._
  def initialBehavior(
      replyTo: ActorRef[InvitationDAOWorkerResponse]
  ): Behavior[InvitationDAOWorkerCommand] =
    Behaviors.setup { ctx =>
      val expectingDAOResponse =
        Behaviors.receiveMessage[InvitationDAOWorkerCommand] {
          case DAOExecutionCompleted() =>
            replyTo ! WorkDone()
            Behaviors.stopped
          case DAOExecutionFailed() =>
            replyTo ! WorkFailed()
            Behaviors.stopped
        }

      val expectingInitialCommand =
        Behaviors.receiveMessage[InvitationDAOWorkerCommand] {
          case Add(invitation) =>
            ctx.pipeToSelf(invitationDAO.addInvitation(invitation)) {
              case Success(_) => DAOExecutionCompleted()
              case Failure(_) => DAOExecutionFailed()
            }
            expectingDAOResponse
          case Remove(invitationID) =>
            ctx.pipeToSelf(invitationDAO.removeInvitation(invitationID)) {
              case Success(_) => DAOExecutionCompleted()
              case Failure(_) => DAOExecutionFailed()
            }
            expectingDAOResponse
        }

      expectingInitialCommand
    }
}
object InvitationDAOWorker {
  sealed trait InvitationDAOWorkerCommand
  case class Add(invitation: Invitation) extends InvitationDAOWorkerCommand
  case class Remove(invitationID: InvitationID)
      extends InvitationDAOWorkerCommand
  case class DAOExecutionCompleted() extends InvitationDAOWorkerCommand
  case class DAOExecutionFailed() extends InvitationDAOWorkerCommand
  sealed trait InvitationDAOWorkerResponse
  case class WorkDone() extends InvitationDAOWorkerResponse
  case class WorkFailed() extends InvitationDAOWorkerResponse

}
