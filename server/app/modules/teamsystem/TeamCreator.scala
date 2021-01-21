package modules.teamsystem

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import com.google.inject.Inject
import models.DiscordID
import models.teamsystem.{Member, MemberStatus, TeamID}

import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
class TeamCreator @Inject() (
    teamCreatorWorker: TeamCreatorWorker,
    memberSupervisor: MemberSupervisor,
    teamMemberAddWorker: TeamMemberAddWorker
) {
  import TeamCreator._
  def initialBehavior(
      userID: DiscordID,
      teamName: String,
      replyTo: ActorRef[CreationResponse]
  ): Behavior[CreationCommand] = {
    Behaviors.setup { ctx =>
      ctx.scheduleOnce(12 seconds, ctx.self, ErrorCreatingTeam())

      val backendResponseCanCreate: ActorRef[MemberSupervisor.MemberResponse] =
        ctx.messageAdapter {
          case MemberSupervisor.Yes() => CanCreate()
          case MemberSupervisor.No()  => CanNotCreate()
        }
      val backendResponseCreationWorker
          : ActorRef[TeamCreatorWorker.CreationResponse] =
        ctx.messageAdapter {
          case TeamCreatorWorker.CreationSuccess(teamID) =>
            TeamCreationEmpty(teamID)
          case TeamCreatorWorker.CreationFailed() => ErrorCreatingTeam()
        }
      val backendResponseMemberAdderWorker
          : ActorRef[TeamMemberAddWorker.TeamMemberAddWorkerResponse] =
        ctx.messageAdapter {
          case TeamMemberAddWorker.WorkDone()   => PrincipalAddedAsMember()
          case TeamMemberAddWorker.WorkFailed() => ErrorCreatingTeam()
        }

      val awaitingMemberWorker = Behaviors.receiveMessage[CreationCommand] {
        case PrincipalAddedAsMember() =>
          replyTo ! CreationDone()
          Behaviors.stopped
        case ErrorCreatingTeam() =>
          replyTo ! CreationFailed()
          Behaviors.stopped
      }

      val awaitingDaoCreation = Behaviors.receiveMessage[CreationCommand] {
        case TeamCreationEmpty(teamID) =>
          val addPrincipalAsOfficialWorker = ctx.spawnAnonymous(
            teamMemberAddWorker.initialBehavior(
              backendResponseMemberAdderWorker
            )
          )
          addPrincipalAsOfficialWorker ! TeamMemberAddWorker.Add(
            Member(userID, MemberStatus.Official),
            teamID
          )
          awaitingMemberWorker
        case ErrorCreatingTeam() =>
          replyTo ! CreationFailed()
          Behaviors.stopped
      }

      val awaitingCreationAvailable =
        Behaviors.receiveMessage[CreationCommand] {
          case CanCreate() =>
            val creationWorker = ctx.spawnAnonymous(
              teamCreatorWorker.initialBehavior(backendResponseCreationWorker)
            )
            creationWorker ! TeamCreatorWorker.Create(userID, teamName)
            awaitingDaoCreation
          case CanNotCreate() =>
            replyTo ! CreationFailed()
            awaitingDaoCreation
          case ErrorCreatingTeam() =>
            replyTo ! CreationFailed()
            Behaviors.stopped
        }

      val awaitingCreateMessage = Behaviors.receiveMessage[CreationCommand] {
        case Create() =>
          val memberQuery: ActorRef[MemberSupervisor.IsOfficial] =
            ctx.spawnAnonymous(memberSupervisor.initialBehavior())
          memberQuery ! MemberSupervisor.IsOfficial(
            userID,
            backendResponseCanCreate
          )
          awaitingCreationAvailable
        case ErrorCreatingTeam() =>
          replyTo ! CreationFailed()
          Behaviors.stopped
      }
      awaitingCreateMessage
    }
  }
}

object TeamCreator {
  sealed trait CreationCommand
  case class Create() extends CreationCommand
  private case class CanCreate() extends CreationCommand
  private case class CanNotCreate() extends CreationCommand
  private case class TeamCreationEmpty(teamID: TeamID) extends CreationCommand
  private case class PrincipalAddedAsMember() extends CreationCommand
  private case class ErrorCreatingTeam() extends CreationCommand

  sealed trait CreationResponse
  case class CreationDone() extends CreationResponse
  case class CreationFailed() extends CreationResponse

}