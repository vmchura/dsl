package modules.teamsystem

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import models.daos.teamsystem.TeamDAO
import models.teamsystem.TeamID

import javax.inject.Inject
import scala.util.{Failure, Success}

class TeamDestroyer @Inject() (teamDAO: TeamDAO) {
  import TeamDestroyer._
  def initialBehavior(
      replyTo: ActorRef[TeamDestroyerResponse]
  ): Behavior[TeamDestroyerCommand] =
    Behaviors.setup { ctx =>
      val pendingDestroy: Behavior[TeamDestroyerCommand] =
        Behaviors.receiveMessage {
          case TeamDAODestroyed() =>
            replyTo ! TeamDestroyed()
            Behaviors.stopped
          case TeamDAOError(reason) =>
            replyTo ! TeamDestroyerError(reason)
            Behaviors.stopped
        }

      Behaviors.receiveMessage {
        case DestroyTeam(teamID) =>
          ctx.pipeToSelf(teamDAO.removeTeam(teamID)) {
            case Success(true)  => TeamDAODestroyed()
            case Success(false) => TeamDAOError("DB Error removing")
            case Failure(exception) =>
              TeamDAOError(s"Cant remove ${exception.getMessage}")
          }
          pendingDestroy
      }
    }

}
object TeamDestroyer {
  sealed trait TeamDestroyerCommand
  case class DestroyTeam(teamID: TeamID) extends TeamDestroyerCommand
  case class TeamDAODestroyed() extends TeamDestroyerCommand
  case class TeamDAOError(reason: String) extends TeamDestroyerCommand

  sealed trait TeamDestroyerResponse
  case class TeamDestroyed() extends TeamDestroyerResponse
  case class TeamDestroyerError(reason: String) extends TeamDestroyerResponse
}
