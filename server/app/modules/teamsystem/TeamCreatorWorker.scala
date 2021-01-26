package modules.teamsystem

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import com.google.inject.Inject
import models.daos.teamsystem.TeamDAO
import models.teamsystem.TeamID
import play.api.libs.concurrent.ActorModule
import shared.models.DiscordID

import scala.util.{Failure, Success}
class TeamCreatorWorker @Inject() (teamDAO: TeamDAO) {
  import TeamCreatorWorker._
  def initialBehavior(
      replyTo: ActorRef[CreationResponse]
  ): Behavior[CreationWorkerCommand] =
    Behaviors.setup { ctx =>
      val expectingDAOResponse =
        Behaviors.receiveMessage[CreationWorkerCommand] {
          case CreationDAOComplete(teamID) =>
            replyTo ! CreationSuccess(teamID)
            Behaviors.stopped
          case CreationDAOFailed() =>
            replyTo ! CreationFailed()
            Behaviors.stopped
        }

      val expectingCreateMessage =
        Behaviors.receiveMessage[CreationWorkerCommand] {
          case Create(user, teamName) =>
            ctx.pipeToSelf(teamDAO.save(user, teamName)) {
              case Success(value) => CreationDAOComplete(value)
              case Failure(_)     => CreationDAOFailed()
            }
            expectingDAOResponse
        }

      expectingCreateMessage
    }
}

object TeamCreatorWorker extends ActorModule {
  sealed trait CreationWorkerCommand
  case class Create(user: DiscordID, teamName: String)
      extends CreationWorkerCommand
  case class CreationDAOComplete(teamID: TeamID) extends CreationWorkerCommand
  case class CreationDAOFailed() extends CreationWorkerCommand
  sealed trait CreationResponse
  case class CreationSuccess(teamID: TeamID) extends CreationResponse
  case class CreationFailed() extends CreationResponse

  override type Message = CreationWorkerCommand

}
