package modules.teamsystem

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import com.google.inject.Inject
import models.DiscordID
import models.daos.teamsystem.TeamDAO
import models.teamsystem.TeamID
import play.api.libs.concurrent.ActorModule

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
import scala.util.{Failure, Success}
class MemberSupervisor @Inject() (teamDAO: TeamDAO) {
  import MemberSupervisor._
  private val behaviourExpectingDAOResponse =
    Behaviors.receiveMessage[MemberQuery] {
      case DAOResponseSuccess(replyTo: ActorRef[MemberResponse]) =>
        replyTo ! Yes()
        Behaviors.stopped
      case DAOResponseFailed(replyTo: ActorRef[MemberResponse]) =>
        replyTo ! No()
        Behaviors.stopped
    }

  def initialBehavior(
  ): Behavior[MemberQuery] = {

    val behaviourExpectingOfficial = Behaviors.setup[MemberQuery] { ctx =>
      def pipeToSelfTimeOut[T](
          f: Future[T],
          replyTo: ActorRef[MemberResponse]
      ): Unit = {
        ctx.pipeToSelf(f) {
          case Success(_) => DAOResponseSuccess(replyTo)
          case Failure(_) => DAOResponseFailed(replyTo)
        }
        ctx.scheduleOnce(10 seconds, ctx.self, DAOResponseFailed(replyTo))
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
  case class DAOResponseSuccess(replyTo: ActorRef[MemberResponse])
      extends DAOResponse
  case class DAOResponseFailed(replyTo: ActorRef[MemberResponse])
      extends DAOResponse

  sealed trait MemberResponse
  case class Yes() extends MemberResponse
  case class No() extends MemberResponse

}
