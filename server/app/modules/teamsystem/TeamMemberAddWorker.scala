package modules.teamsystem

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import com.google.inject.Inject
import models.daos.teamsystem.TeamDAO
import models.teamsystem.{Member, TeamID}

import scala.util.{Failure, Success}

class TeamMemberAddWorker @Inject() (teamDAO: TeamDAO) {
  import TeamMemberAddWorker._
  def initialBehavior(
      replyTo: ActorRef[TeamMemberAddWorkerResponse]
  ): Behavior[TeamMemberAddWorkerCommand] =
    Behaviors.setup { ctx =>
      val expectingDAOResponse =
        Behaviors.receiveMessage[TeamMemberAddWorkerCommand] {
          case CreationDAOComplete() =>
            replyTo ! WorkDone()
            Behaviors.stopped
          case CreationDAOFailed() =>
            replyTo ! WorkFailed()
            Behaviors.stopped
        }

      val expectingCreateMessage =
        Behaviors.receiveMessage[TeamMemberAddWorkerCommand] {
          case Add(member, teamID) =>
            ctx.pipeToSelf(teamDAO.addMemberTo(member, teamID)) {
              case Success(_) => CreationDAOComplete()
              case Failure(_) => CreationDAOFailed()
            }
            expectingDAOResponse
        }

      expectingCreateMessage
    }
}
object TeamMemberAddWorker {
  sealed trait TeamMemberAddWorkerCommand
  case class Add(member: Member, teamID: TeamID)
      extends TeamMemberAddWorkerCommand
  case class CreationDAOComplete() extends TeamMemberAddWorkerCommand
  case class CreationDAOFailed() extends TeamMemberAddWorkerCommand
  sealed trait TeamMemberAddWorkerResponse
  case class WorkDone() extends TeamMemberAddWorkerResponse
  case class WorkFailed() extends TeamMemberAddWorkerResponse

}
