package modules.teamsystem

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import com.google.inject.Inject
import models.DiscordID
import models.daos.teamsystem.TeamDAO
import models.teamsystem.{Member, MemberStatus, TeamID}
import modules.teamsystem

import scala.util.{Failure, Success}

class TeamManager @Inject() (
    teamMemberAddWorker: TeamMemberAddWorker,
    teamDestroyer: TeamDestroyer,
    teamDAO: TeamDAO,
    memberSupervisor: MemberSupervisor
) {
  import TeamManager._
  def initialBehavior(
      replyTo: ActorRef[TeamManagerResponse]
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
        case AddUserToAsOfficial(userID, teamID) =>
          val worker = ctx.spawnAnonymous(
            teamMemberAddWorker.initialBehavior(backendResponseDAO)
          )
          worker ! TeamMemberAddWorker.Add(
            Member(userID, MemberStatus.Official),
            teamID
          )
          awaitingDAOCompletition
        case AddUserToAsSuplente(userID, teamID) =>
          val worker = ctx.spawnAnonymous(
            teamMemberAddWorker.initialBehavior(backendResponseDAO)
          )
          worker ! TeamMemberAddWorker.Add(
            Member(userID, MemberStatus.Suplente),
            teamID
          )
          awaitingDAOCompletition
        case RemoveUserFrom(userID, teamID) =>
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
}
object TeamManager {
  sealed trait TeamManagerCommand
  case class AddUserToAsOfficial(
      userID: DiscordID,
      teamID: TeamID
  ) extends TeamManagerCommand
  case class AddUserToAsSuplente(
      userID: DiscordID,
      teamID: TeamID
  ) extends TeamManagerCommand
  case class RemoveUserFrom(
      userID: DiscordID,
      teamID: TeamID
  ) extends TeamManagerCommand

  case class DAOCompleted() extends TeamManagerCommand
  case class MemberRemovingIsOfficial() extends TeamManagerCommand
  case class MemberRemovingIsNotOfficial() extends TeamManagerCommand
  case class UserRemovingIsNotMember() extends TeamManagerCommand
  case class ErrorOnManaging() extends TeamManagerCommand
  case class MemberRemoved() extends TeamManagerCommand

  sealed trait TeamManagerResponse
  case class Done() extends TeamManagerResponse
  case class Failed() extends TeamManagerResponse
}
