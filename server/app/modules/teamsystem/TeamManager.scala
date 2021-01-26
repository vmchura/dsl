package modules.teamsystem

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import com.google.inject.Provides
import models.daos.teamsystem.TeamDAO
import models.teamsystem.{Member, MemberStatus, TeamID}
import modules.teamsystem
import play.api.libs.concurrent.ActorModule
import shared.models.DiscordID

import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
import scala.util.{Failure, Success}

object TeamManager extends ActorModule {
  sealed trait TeamManagerCommand {
    def replyTo: ActorRef[TeamManagerResponse]
  }

  override type Message = TeamManagerCommand
  case class AddUserToAsOfficial(
      userID: DiscordID,
      teamID: TeamID,
      replyTo: ActorRef[TeamManagerResponse]
  ) extends TeamManagerCommand
  case class AddUserToAsSuplente(
      userID: DiscordID,
      teamID: TeamID,
      replyTo: ActorRef[TeamManagerResponse]
  ) extends TeamManagerCommand
  case class RemoveUserFrom(
      userID: DiscordID,
      teamID: TeamID,
      replyTo: ActorRef[TeamManagerResponse]
  ) extends TeamManagerCommand

  trait InternalCommand extends TeamManagerCommand {
    override def replyTo: ActorRef[TeamManagerResponse] =
      throw new NotImplementedError()
  }

  case class DAOCompleted() extends InternalCommand
  case class MemberRemovingIsOfficial() extends InternalCommand
  case class MemberRemovingIsNotOfficial() extends InternalCommand
  case class UserRemovingIsNotMember() extends InternalCommand
  case class ErrorOnManaging() extends InternalCommand
  case class MemberRemoved() extends InternalCommand
  case class TeamManagerExternalDone(worker: ActorRef[TeamManagerCommand])
      extends InternalCommand
  case class TeamManagerExternalTimeOut(
      worker: ActorRef[TeamManagerCommand],
      replyTo: ActorRef[TeamManagerResponse]
  ) extends TeamManagerCommand

  sealed trait TeamManagerResponse
  case class Done() extends TeamManagerResponse
  case class Failed() extends TeamManagerResponse

  def initialBehavior(
      replyTo: ActorRef[TeamManagerResponse]
  )(implicit
      teamDestroyer: TeamDestroyer,
      teamDAO: TeamDAO,
      teamMemberAddWorker: TeamMemberAddWorker,
      memberSupervisor: MemberSupervisor
  ): Behavior[TeamManagerCommand] = {
    Behaviors.setup { ctx =>
      val backendResponseDAO: ActorRef[
        teamsystem.TeamMemberAddWorker.TeamMemberAddWorkerResponse
      ] =
        ctx.messageAdapter {
          case teamsystem.TeamMemberAddWorker.WorkDone()   => DAOCompleted()
          case teamsystem.TeamMemberAddWorker.WorkFailed() => ErrorOnManaging()
        }

      val awaitingDAOCompletition =
        Behaviors.receiveMessage[TeamManagerCommand] {
          case DAOCompleted() =>
            replyTo ! Done()
            Behaviors.stopped
          case ErrorOnManaging() =>
            replyTo ! Failed()
            Behaviors.stopped
        }

      val awaitingMemberRemoval: Behavior[TeamManagerCommand] =
        Behaviors.receiveMessage {
          case MemberRemoved() =>
            replyTo ! Done()
            Behaviors.stopped
          case ErrorOnManaging() =>
            replyTo ! Failed()
            Behaviors.stopped
        }

      def awaitingMemberPositionResponse(
          userID: DiscordID,
          teamID: TeamID
      ): Behavior[TeamManagerCommand] =
        Behaviors.receiveMessage {
          case MemberRemovingIsOfficial() =>
            val worker = ctx.spawnAnonymous(
              teamDestroyer.initialBehavior(ctx.messageAdapter {
                case TeamDestroyer.TeamDestroyed()       => MemberRemoved()
                case TeamDestroyer.TeamDestroyerError(_) => ErrorOnManaging()
              })
            )

            worker ! TeamDestroyer.DestroyTeam(teamID)
            awaitingMemberRemoval
          case MemberRemovingIsNotOfficial() =>
            ctx.pipeToSelf(teamDAO.removeMember(userID, teamID)) {
              case Success(true)  => MemberRemoved()
              case Success(false) => ErrorOnManaging()
              case Failure(_)     => ErrorOnManaging()
            }
            awaitingMemberRemoval
          case UserRemovingIsNotMember() =>
            ctx.self ! MemberRemoved()
            awaitingMemberRemoval
        }

      val awaitingCommand = Behaviors.receiveMessage[TeamManagerCommand] {
        case AddUserToAsOfficial(userID, teamID, _) =>
          val worker = ctx.spawnAnonymous(
            teamMemberAddWorker.initialBehavior(backendResponseDAO)
          )
          worker ! TeamMemberAddWorker.Add(
            Member(userID, MemberStatus.Official),
            teamID
          )
          awaitingDAOCompletition
        case AddUserToAsSuplente(userID, teamID, _) =>
          val worker = ctx.spawnAnonymous(
            teamMemberAddWorker.initialBehavior(backendResponseDAO)
          )
          worker ! TeamMemberAddWorker.Add(
            Member(userID, MemberStatus.Suplente),
            teamID
          )
          awaitingDAOCompletition
        case RemoveUserFrom(userID, teamID, _) =>
          val worker = ctx.spawnAnonymous(memberSupervisor.initialBehavior())
          worker ! MemberSupervisor.IsPrincipal(
            userID,
            teamID,
            ctx.messageAdapter {
              case MemberSupervisor.Yes() =>
                MemberRemovingIsOfficial()
              case MemberSupervisor.No() =>
                MemberRemovingIsNotOfficial()
            }
          )
          awaitingMemberPositionResponse(userID, teamID)
        case ErrorOnManaging() =>
          replyTo ! Failed()
          Behaviors.stopped
      }

      awaitingCommand
    }
  }

  @Provides
  def apply(
  )(implicit
      teamDestroyer: TeamDestroyer,
      teamDAO: TeamDAO,
      teamMemberAddWorker: TeamMemberAddWorker,
      memberSupervisor: MemberSupervisor
  ): Behavior[TeamManagerCommand] =
    Behaviors.setup { ctx =>
      var workers = Set.empty[ActorRef[TeamManagerCommand]]

      Behaviors.receiveMessage[TeamManagerCommand] {
        case TeamManagerExternalDone(worker) =>
          workers = workers - worker
          Behaviors.same
        case TeamManagerExternalTimeOut(worker, replyTo) =>
          if (workers.contains(worker)) {
            replyTo ! Failed()
            ctx.stop(worker)
          }

          workers = workers - worker

          Behaviors.same
        case otherCommand =>
          val actor = ctx.spawnAnonymous(initialBehavior(otherCommand.replyTo))
          ctx.watchWith(actor, TeamManagerExternalDone(actor))
          ctx.scheduleOnce(
            12 seconds,
            ctx.self,
            TeamManagerExternalTimeOut(actor, otherCommand.replyTo)
          )
          workers = workers + actor
          actor ! otherCommand
          Behaviors.same

      }

    }
}
