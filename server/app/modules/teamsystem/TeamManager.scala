package modules.teamsystem

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import com.google.inject.Inject
import models.DiscordID
import models.teamsystem.{Member, MemberStatus, TeamID}
import modules.teamsystem

class TeamManager @Inject() (teamMemberAddWorker: TeamMemberAddWorker) {
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
          awaitingDAOCompletition
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
  case class ErrorOnManaging() extends TeamManagerCommand

  sealed trait TeamManagerResponse
  case class Done() extends TeamManagerResponse
  case class Failed() extends TeamManagerResponse
}
