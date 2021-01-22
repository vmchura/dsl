package modules.teamsystem

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import com.google.inject.Inject
import models.DiscordID
import models.daos.teamsystem.InvitationDAO
import models.teamsystem.{Invitation, InvitationID, MemberStatus, TeamID}

import java.util.UUID
import scala.util.{Failure, Success}

class InvitationManager @Inject() (
    invitationDAOWorker: InvitationDAOWorker,
    invitationDAO: InvitationDAO,
    teamManager: TeamManager,
    memberSupervisor: MemberSupervisor
) {

  import InvitationManager._

  def initialBehavior(
      replyTo: ActorRef[InvitationManagerResponse]
  ): Behavior[InvitationCommand] = {

    Behaviors.setup { ctx =>
      def pendingInvRemoval(
          onSuccess: InvitationManagerResponse,
          messageOnFailure: String
      ): Behavior[InvitationCommand] =
        Behaviors.receiveMessage {
          case InvitationDAORemoved() =>
            replyTo ! onSuccess
            Behaviors.stopped
          case InvitationManagerError(reason) =>
            replyTo ! InvitationError(s"$messageOnFailure : $reason")
            Behaviors.stopped
        }

      def backendResponseDAO(
          onSuccess: InvitationCommand,
          messageOnError: String
      ): ActorRef[InvitationDAOWorker.InvitationDAOWorkerResponse] =
        ctx.messageAdapter {
          case InvitationDAOWorker.WorkDone() => onSuccess
          case InvitationDAOWorker.WorkFailed() =>
            InvitationManagerError(messageOnError)
        }

      def backendMemberQuery(
          messageOnYes: InvitationCommand,
          messageOnNo: InvitationCommand
      ): ActorRef[MemberSupervisor.MemberResponse] =
        ctx.messageAdapter {
          case MemberSupervisor.Yes() => messageOnYes
          case MemberSupervisor.No()  => messageOnNo
        }
      def backendTeamManager(
          messageAddapted: InvitationCommand
      ): ActorRef[TeamManager.TeamManagerResponse] =
        ctx.messageAdapter {
          case TeamManager.Done() => messageAddapted
          case TeamManager.Failed() =>
            InvitationManagerError(
              "Los criterios no se cumplen para agregar al usuario al team"
            )
        }
      def pendingMemberAdding(
          invitationID: InvitationID
      ): Behavior[InvitationCommand] =
        Behaviors.receiveMessage {
          case MemberAdded() =>
            val worker = ctx.spawnAnonymous(
              invitationDAOWorker.initialBehavior(
                backendResponseDAO(
                  InvitationDAORemoved(),
                  "Member Added but Invitation not removed"
                )
              )
            )
            worker ! InvitationDAOWorker.Remove(invitationID)
            pendingInvRemoval(InvitationAccepted(), "")
          case InvitationManagerError(reason) =>
            replyTo ! InvitationError(reason)
            Behaviors.stopped
        }
      def invitationNotChecked(
          invitation: Invitation
      ): Behavior[InvitationCommand] =
        Behaviors.receiveMessage {
          case InvitationIsValid() =>
            val worker = ctx.spawnAnonymous(
              teamManager.initialBehavior(backendTeamManager(MemberAdded()))
            )
            invitation.status match {
              case MemberStatus.Official =>
                worker ! TeamManager.AddUserToAsOfficial(
                  invitation.to,
                  invitation.teamID
                )
              case MemberStatus.Suplente =>
                worker ! TeamManager.AddUserToAsSuplente(
                  invitation.to,
                  invitation.teamID
                )
            }
            pendingMemberAdding(invitation.invitationID)
          case InvitationIsInvalid() =>
            val worker = ctx.spawnAnonymous(
              invitationDAOWorker.initialBehavior(
                backendResponseDAO(InvitationDAORemoved(), "")
              )
            )
            worker ! InvitationDAOWorker.Remove(invitation.invitationID)
            pendingInvRemoval(
              InvitationError("Invitation is not valid anymore"),
              "Invitation is not valid and cant be removed"
            )
        }

      val invitationLoading: Behavior[InvitationCommand] =
        Behaviors.receiveMessage {
          case InvitationLoaded(invitation) =>
            val worker = ctx.spawnAnonymous(memberSupervisor.initialBehavior())
            val backendAdapter =
              backendMemberQuery(InvitationIsInvalid(), InvitationIsValid())
            invitation.status match {
              case MemberStatus.Official =>
                worker ! MemberSupervisor.IsOfficial(
                  invitation.to,
                  backendAdapter
                )
              case MemberStatus.Suplente =>
                worker ! MemberSupervisor.IsMemberFrom(
                  invitation.to,
                  invitation.teamID,
                  backendAdapter
                )
            }

            invitationNotChecked(invitation)
          case InvitationManagerError(reason) =>
            replyTo ! InvitationError(reason)
            Behaviors.stopped
        }

      val pendingInvitationSave: Behavior[InvitationCommand] =
        Behaviors.receiveMessage {
          case InvitationSaved() =>
            replyTo ! InvitationMade()
            Behaviors.stopped
          case InvitationManagerError(reason) =>
            replyTo ! InvitationError(reason)
            Behaviors.stopped
        }

      def metaNotChecked(
          from: DiscordID,
          to: DiscordID,
          teamID: TeamID,
          position: MemberStatus
      ): Behavior[InvitationCommand] =
        Behaviors.receiveMessage {
          case MetaInvitationIsValid() =>
            val worker = ctx.spawnAnonymous(
              invitationDAOWorker.initialBehavior(
                backendResponseDAO(
                  InvitationSaved(),
                  "Error al guardar la invitación"
                )
              )
            )
            worker ! InvitationDAOWorker.Add(
              Invitation(
                InvitationID(UUID.randomUUID()),
                from,
                to,
                teamID,
                position
              )
            )
            pendingInvitationSave
          case MetaInvitationIsInvalid() =>
            replyTo ! InvitationError("Invitation no cumple los criterios")
            Behaviors.stopped
        }

      Behaviors.receiveMessage[InvitationCommand] {
        case Invite(from, to, teamID, position) =>
          val worker = ctx.spawnAnonymous(memberSupervisor.initialBehavior())
          val backendAdapter =
            backendMemberQuery(
              MetaInvitationIsInvalid(),
              MetaInvitationIsValid()
            )
          worker ! (position match {
            case MemberStatus.Official =>
              MemberSupervisor.IsOfficial(to, backendAdapter)
            case MemberStatus.Suplente =>
              MemberSupervisor.IsMemberFrom(to, teamID, backendAdapter)
          })

          metaNotChecked(from, to, teamID, position)

        case RemoveInvitation(invitationID) =>
          val worker = ctx.spawnAnonymous(
            invitationDAOWorker.initialBehavior(
              backendResponseDAO(
                InvitationDAORemoved(),
                "Invitation not removed"
              )
            )
          )
          worker ! InvitationDAOWorker.Remove(invitationID)
          pendingInvRemoval(InvitationRemoved(), "Error removing invitation")
        case AcceptInvitation(invitationID) =>
          ctx.pipeToSelf(invitationDAO.loadInvitation(invitationID)) {
            case Success(Some(invitation)) =>
              InvitationLoaded(invitation)
            case Success(None) =>
              InvitationManagerError("Cant load invitation")
            case Failure(exception) =>
              InvitationManagerError(
                s"Cant load invitation from DB ${exception.getMessage}"
              )
          }
          invitationLoading
      }

    }
  }
}
object InvitationManager {
  sealed trait InvitationCommand
  case class Invite(
      from: DiscordID,
      to: DiscordID,
      teamID: TeamID,
      position: MemberStatus
  ) extends InvitationCommand
  case class AcceptInvitation(invitationID: InvitationID)
      extends InvitationCommand
  case class RemoveInvitation(invitationID: InvitationID)
      extends InvitationCommand

  case class InvitationLoaded(invitation: Invitation) extends InvitationCommand
  case class MetaInvitationIsValid() extends InvitationCommand
  case class MetaInvitationIsInvalid() extends InvitationCommand
  case class InvitationIsValid() extends InvitationCommand
  case class InvitationIsInvalid() extends InvitationCommand
  case class InvitationSaved() extends InvitationCommand
  case class MemberAdded() extends InvitationCommand
  case class InvitationDAORemoved() extends InvitationCommand
  case class InvitationManagerError(reason: String) extends InvitationCommand

  sealed trait InvitationManagerResponse
  case class InvitationMade() extends InvitationManagerResponse
  case class InvitationAccepted() extends InvitationManagerResponse
  case class InvitationRemoved() extends InvitationManagerResponse
  case class InvitationError(reason: String) extends InvitationManagerResponse

}
