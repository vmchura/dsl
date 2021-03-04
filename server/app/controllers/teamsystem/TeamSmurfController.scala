package controllers.teamsystem

import akka.actor.typed.ActorRef
import akka.util.Timeout
import controllers.{AbstractAuthController, SilhouetteControllerComponents}
import models.daos.teamsystem.{TeamUserSmurfDAO, TeamUserSmurfPendingDAO}
import models.teamsystem.PendingSmurf
import modules.teamsystem.UniqueSmurfWatcher
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent}
import shared.models.ReplayTeamID

import java.util.UUID
import javax.inject.Inject
import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps
import akka.actor.typed.scaladsl.AskPattern._
import models.Smurf

class TeamSmurfController @Inject() (
    scc: SilhouetteControllerComponents,
    teamUserSmurfPendingDAO: TeamUserSmurfPendingDAO,
    teamUserSmurfDAO: TeamUserSmurfDAO,
    uniqueSmurfWatcher: ActorRef[UniqueSmurfWatcher.Command]
)(implicit
    ex: ExecutionContext,
    scheduler: akka.actor.typed.Scheduler
) extends AbstractAuthController(scc)
    with I18nSupport {
  implicit val timeOut: Timeout = 10 seconds
  private def removePending(replayTeamID: ReplayTeamID): Future[Boolean] =
    teamUserSmurfPendingDAO.removeRelated(replayTeamID)

  def removeSmurf(replayTeamID: UUID): Action[AnyContent] =
    silhouette.SecuredAction.async { implicit request =>
      val result = Redirect(
        controllers.teamsystem.routes.TeamManagerController.showMyTeams()
      )
      removePending(ReplayTeamID(replayTeamID)).map { res =>
        if (res) {
          result.flashing("success" -> "smurf removed")
        } else {
          result.flashing("error" -> "smurf removal failed")
        }
      }
    }

  def acceptSmurf(replayTeamID: UUID): Action[AnyContent] =
    silhouette.SecuredAction.async { implicit request =>
      val result = Redirect(
        controllers.teamsystem.routes.TeamManagerController.showMyTeams()
      )
      def smurfAdd(pendingSmurf: PendingSmurf): Future[Boolean] =
        teamUserSmurfDAO.add(pendingSmurf.discordID, pendingSmurf.smurf)

      def smurfIsUnique(smurf: Smurf): Future[Boolean] =
        uniqueSmurfWatcher
          .ask[UniqueSmurfWatcher.Response](ref =>
            UniqueSmurfWatcher.LocateOwner(smurf, ref)
          )
          .map {
            case UniqueSmurfWatcher.SmurfNotAssigned() => true
            case _                                     => false
          }

      implicit class BooleanWithError(fut: Boolean) {
        def withError(error: String): Future[Boolean] =
          if (fut) Future.successful(fut)
          else Future.failed(new IllegalStateException(error))
      }

      val message = (for {
        listPending <-
          teamUserSmurfPendingDAO.loadRelated(ReplayTeamID(replayTeamID))
        nonEmptyList <- Future.successful(listPending.nonEmpty)
        _ <- nonEmptyList.withError("No se encuentra el smurf")
        isUnique <- smurfIsUnique(listPending.head.smurf)
        _ <- isUnique.withError("El smurf no es único")
        added <- smurfAdd(listPending.head)
        _ <- added.withError("El smurf no se ha podido añadir")
        removed <- removePending(listPending.head.replayTeamID)
        _ <- removed.withError("Smurf se añadió pero no pudo removerse")
      } yield {
        "success" -> "smurf añadido"
      }).recoverWith(error => Future.successful("error" -> error.getMessage))

      message.map { f => result.flashing(f) }

    }
}
