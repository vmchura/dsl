package modules.teamsystem

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import com.google.inject.{Inject, Provides}
import models.daos.teamsystem.InvitationDAO
import models.teamsystem.{Invitation, InvitationID, MemberStatus, TeamID}
import play.api.libs.concurrent.ActorModule
import shared.models.DiscordID

import java.util.UUID
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
import scala.util.{Failure, Success}

object InvitationManager extends ActorModule {
  sealed trait InvitationCommand {
    def replyTo: ActorRef[InvitationManagerResponse]
  }

  override type Message = InvitationCommand
  case class Invite(
      from: DiscordID,
      to: DiscordID,
      teamID: TeamID,
      position: MemberStatus,
      replyTo: ActorRef[InvitationManagerResponse]
  ) extends InvitationCommand
  case class AcceptInvitation(
      invitationID: InvitationID,
      replyTo: ActorRef[InvitationManagerResponse]
  ) extends InvitationCommand
  case class RemoveInvitation(
      invitationID: InvitationID,
      replyTo: ActorRef[InvitationManagerResponse]
  ) extends InvitationCommand

  trait InternalCommand extends InvitationCommand {
    def replyTo: ActorRef[InvitationManagerResponse] =
      throw new NotImplementedError()
  }

  case class InvitationLoaded(invitation: Invitation) extends InternalCommand
  case class MetaInvitationIsValid() extends InternalCommand
  case class MetaInvitationIsInvalid() extends InternalCommand
  case class InvitationIsValid() extends InternalCommand
  case class InvitationIsInvalid() extends InternalCommand
  case class InvitationSaved() extends InternalCommand
  case class MemberAdded() extends InternalCommand
  case class InvitationDAORemoved() extends InternalCommand
  case class InvitationManagerError(reason: String) extends InternalCommand

  case class InvitationExternalDone(worker: ActorRef[InvitationCommand])
      extends InternalCommand
  case class InvitationExternalTimeOut(
      worker: ActorRef[InvitationCommand],
      replyTo: ActorRef[InvitationManagerResponse]
  ) extends InvitationCommand

  sealed trait InvitationManagerResponse
  case class InvitationMade() extends InvitationManagerResponse
  case class InvitationAccepted() extends InvitationManagerResponse
  case class InvitationRemoved() extends InvitationManagerResponse
  case class InvitationError(reason: String) extends InvitationManagerResponse

  @Provides
  def apply(
      invitationDAOWorker: InvitationDAOWorker,
      invitationDAO: InvitationDAO,
      teamManager: ActorRef[TeamManager.TeamManagerCommand],
      memberSupervisor: MemberSupervisor
  ) = {

    def initialBehavior(
        replyTo: ActorRef[InvitationManagerResponse]
    ): Behavior[InvitationCommand] = {
      Behaviors.setup[InvitationCommand] { ctx =>
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
              invitation.status match {
                case MemberStatus.Official =>
                  teamManager ! TeamManager.AddUserToAsOfficial(
                    invitation.to,
                    invitation.teamID,
                    backendTeamManager(MemberAdded())
                  )
                case MemberStatus.Suplente =>
                  teamManager ! TeamManager.AddUserToAsSuplente(
                    invitation.to,
                    invitation.teamID,
                    backendTeamManager(MemberAdded())
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
              val worker =
                ctx.spawnAnonymous(memberSupervisor.initialBehavior())
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
          case Invite(from, to, teamID, position, _) =>
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

          case RemoveInvitation(invitationID, _) =>
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
          case AcceptInvitation(invitationID, _) =>
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

    Behaviors.setup[InvitationCommand] { ctx =>
      var workers = Set.empty[ActorRef[InvitationCommand]]

      Behaviors.receiveMessage[InvitationCommand] {
        case InvitationExternalDone(worker) =>
          workers = workers - worker
          Behaviors.same
        case InvitationExternalTimeOut(worker, replyTo) =>
          if (workers.contains(worker)) {
            replyTo ! InvitationError("Time out")
            ctx.stop(worker)
          }

          workers = workers - worker

          Behaviors.same
        case otherCommand =>
          val actor = ctx.spawnAnonymous(initialBehavior(otherCommand.replyTo))
          ctx.watchWith(actor, InvitationExternalDone(actor))
          ctx.scheduleOnce(
            12 seconds,
            ctx.self,
            InvitationExternalTimeOut(actor, otherCommand.replyTo)
          )
          workers = workers + actor
          actor ! otherCommand
          Behaviors.same

      }

    }
  }

}
