package modules.teamsystem

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import com.google.inject.Inject
import models.daos.teamsystem.TeamDAO
import models.teamsystem.TeamID
import play.api.libs.concurrent.ActorModule
import shared.models.DiscordID

import scala.concurrent.Future
import scala.language.postfixOps
import scala.util.{Failure, Success}
class MemberSupervisor @Inject() (teamDAO: TeamDAO) {
  import MemberSupervisor._
  private val behaviourExpectingDAOResponse =
    Behaviors.receiveMessage[MemberQuery] {
      case DAOResponseSuccess(true, replyTo: ActorRef[MemberResponse]) =>
        replyTo ! Yes()
        Behaviors.stopped
      case DAOResponseSuccess(false, replyTo: ActorRef[MemberResponse]) =>
        replyTo ! No()
        Behaviors.stopped
      case DAOResponseFailed(reason, replyTo: ActorRef[MemberResponse]) =>
        replyTo ! ErrorOnQuery(reason)
        Behaviors.stopped
    }

  def initialBehavior(
  ): Behavior[MemberQuery] = {

    val behaviourExpectingOfficial = Behaviors.setup[MemberQuery] { ctx =>
      def pipeToSelfTimeOut(
          f: Future[Boolean],
          replyTo: ActorRef[MemberResponse]
      ): Unit = {
        ctx.pipeToSelf(f) {
          case Success(response) => DAOResponseSuccess(response, replyTo)
          case Failure(error)    => DAOResponseFailed(error.getMessage, replyTo)
        }
      }

      Behaviors.receiveMessage {
        case IsOfficial(discordID, replyTo) =>
          pipeToSelfTimeOut(teamDAO.isOfficial(discordID), replyTo)
          behaviourExpectingDAOResponse
        case IsOfficialFrom(discordID, teamID, replyTo) =>
          pipeToSelfTimeOut(teamDAO.isOfficial(discordID, teamID), replyTo)
          behaviourExpectingDAOResponse
        case IsMemberFrom(discordID, teamID, replyTo) =>
          pipeToSelfTimeOut(teamDAO.isMember(discordID, teamID), replyTo)
          behaviourExpectingDAOResponse
        case IsPrincipal(discordID, teamID, replyTo) =>
          pipeToSelfTimeOut(teamDAO.isPrincipal(discordID, teamID), replyTo)
          behaviourExpectingDAOResponse
      }

    }
    behaviourExpectingOfficial

  }
}

object MemberSupervisor extends ActorModule {
  sealed trait MemberQuery
  case class IsPrincipal(
      discordID: DiscordID,
      teamID: TeamID,
      replyTo: ActorRef[MemberResponse]
  ) extends MemberQuery
  case class IsOfficial(discordID: DiscordID, replyTo: ActorRef[MemberResponse])
      extends MemberQuery
  case class IsOfficialFrom(
      discordID: DiscordID,
      teamID: TeamID,
      replyTo: ActorRef[MemberResponse]
  ) extends MemberQuery
  case class IsMemberFrom(
      discordID: DiscordID,
      teamID: TeamID,
      replyTo: ActorRef[MemberResponse]
  ) extends MemberQuery
  sealed trait DAOResponse extends MemberQuery
  case class DAOResponseSuccess(
      queryResponse: Boolean,
      replyTo: ActorRef[MemberResponse]
  ) extends DAOResponse
  case class DAOResponseFailed(
      reason: String,
      replyTo: ActorRef[MemberResponse]
  ) extends DAOResponse

  sealed trait MemberResponse
  case class Yes() extends MemberResponse
  case class No() extends MemberResponse
  case class ErrorOnQuery(reason: String) extends MemberResponse

}
