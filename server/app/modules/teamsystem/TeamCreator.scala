package modules.teamsystem

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import com.google.inject.Provides
import models.teamsystem.{Member, MemberStatus, TeamID}
import play.api.libs.concurrent.{ActorModule, AkkaGuiceSupport}
import shared.models.DiscordID

import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

object TeamCreator extends ActorModule with AkkaGuiceSupport {

  sealed trait CreationCommand
  case class CreateTeam(
      replyTo: ActorRef[CreationResponse],
      userID: DiscordID,
      teamName: String
  ) extends CreationCommand
  case class TeamCreatorDone(actor: ActorRef[CreationCommand])
      extends CreationCommand
  case class TeamCreatorTimeOut(actor: ActorRef[CreationCommand])
      extends CreationCommand

  case class Create(replyTo: ActorRef[CreationResponse]) extends CreationCommand
  private case class CanCreate() extends CreationCommand
  private case class CanNotCreate() extends CreationCommand
  private case class TeamCreationEmpty(teamID: TeamID) extends CreationCommand
  private case class PrincipalAddedAsMember() extends CreationCommand
  private case class ErrorCreatingTeam() extends CreationCommand

  sealed trait CreationResponse
  case class CreationDone() extends CreationResponse
  case class CreationFailed() extends CreationResponse

  override type Message = CreationCommand

  @Provides
  def apply(
      teamCreatorWorker: TeamCreatorWorker,
      memberSupervisor: MemberSupervisor,
      teamMemberAddWorker: TeamMemberAddWorker
  ): Behavior[CreationCommand] =
    Behaviors.setup { ctx =>
      def initialBehavior(
          userID: DiscordID,
          teamName: String
      ): Behavior[CreationCommand] = {
        Behaviors.setup { ctx =>
          val backendResponseCanCreate
              : ActorRef[MemberSupervisor.MemberResponse] =
            ctx.messageAdapter {
              case MemberSupervisor.Yes() => CanNotCreate()
              case MemberSupervisor.No()  => CanCreate()
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

          def awaitingMemberWorker(replyTo: ActorRef[CreationResponse]) =
            Behaviors.receiveMessage[CreationCommand] {
              case PrincipalAddedAsMember() =>
                replyTo ! CreationDone()
                Behaviors.stopped
              case ErrorCreatingTeam() =>
                replyTo ! CreationFailed()
                Behaviors.stopped
            }

          def awaitingDaoCreation(replyTo: ActorRef[CreationResponse]) =
            Behaviors.receiveMessage[CreationCommand] {
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
                awaitingMemberWorker(replyTo)
              case ErrorCreatingTeam() =>
                replyTo ! CreationFailed()
                Behaviors.stopped
            }

          def awaitingCreationAvailable(replyTo: ActorRef[CreationResponse]) =
            Behaviors.receiveMessage[CreationCommand] {
              case CanCreate() =>
                val creationWorker = ctx.spawnAnonymous(
                  teamCreatorWorker.initialBehavior(
                    backendResponseCreationWorker
                  )
                )
                creationWorker ! TeamCreatorWorker.Create(userID, teamName)
                awaitingDaoCreation(replyTo)
              case CanNotCreate() =>
                replyTo ! CreationFailed()
                awaitingDaoCreation(replyTo)
              case ErrorCreatingTeam() =>
                replyTo ! CreationFailed()
                Behaviors.stopped
            }

          val awaitingCreateMessage =
            Behaviors.receiveMessage[CreationCommand] {
              case Create(replyTo) =>
                val memberQuery: ActorRef[MemberSupervisor.IsOfficial] =
                  ctx.spawnAnonymous(memberSupervisor.initialBehavior())
                memberQuery ! MemberSupervisor.IsOfficial(
                  userID,
                  backendResponseCanCreate
                )
                awaitingCreationAvailable(replyTo)
            }
          awaitingCreateMessage
        }
      }
      var workers = Set.empty[ActorRef[CreationCommand]]
      Behaviors.receiveMessage[CreationCommand] {
        case CreateTeam(replyTo, userID, teamName) =>
          val actor = ctx.spawnAnonymous(initialBehavior(userID, teamName))
          ctx.watchWith(actor, TeamCreatorDone(actor))
          ctx.scheduleOnce(12 seconds, ctx.self, TeamCreatorTimeOut(actor))
          workers = workers + actor
          actor ! Create(replyTo)
          Behaviors.same
        case TeamCreatorDone(worker) =>
          workers = workers - worker
          Behaviors.same
        case TeamCreatorTimeOut(worker) =>
          if (workers.contains(worker))
            ctx.stop(worker)

          workers = workers - worker

          Behaviors.same
      }
    }
}
