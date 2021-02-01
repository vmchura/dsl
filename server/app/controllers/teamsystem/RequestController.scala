package controllers.teamsystem

import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.AskPattern._
import akka.util.Timeout
import com.mohiva.play.silhouette.api.actions.SecuredRequest
import controllers.{
  AbstractAuthController,
  AssetsFinder,
  SilhouetteControllerComponents
}
import models.daos.DiscordPlayerLoggedDAO
import models.daos.teamsystem.TeamDAO

import models.teamsystem.{RequestJoinID, TeamID}
import modules.teamsystem._
import play.api.i18n.I18nSupport
import play.api.mvc._
import shared.models.DiscordID

import java.util.UUID
import javax.inject._
import scala.concurrent.duration.DurationInt
import scala.concurrent.ExecutionContext
import scala.language.postfixOps

class RequestController @Inject() (
    scc: SilhouetteControllerComponents,
    requestManager: ActorRef[RequestJoinManager.InternalCommand]
)(implicit
    assets: AssetsFinder,
    ex: ExecutionContext,
    discordPlayerLoggedDAO: DiscordPlayerLoggedDAO,
    scheduler: akka.actor.typed.Scheduler,
    teamDAO: TeamDAO
) extends AbstractAuthController(scc)
    with I18nSupport {
  implicit val timeOut: Timeout = 10 seconds
  def doRequest(teamID: UUID): Action[AnyContent] =
    silhouette.SecuredAction.async {
      implicit request: SecuredRequest[EnvType, AnyContent] =>
        val result = Redirect(routes.TeamManagerController.showAllTeams())
        val from =
          DiscordID(request.authenticator.loginInfo.providerKey)
        requestManager
          .ask[RequestJoinManager.RequestJoinResponse](ref =>
            RequestJoinManager.RequestJoinCommand(
              from,
              TeamID(teamID),
              ref
            )
          )
          .map {
            case RequestJoinManager.RequestSuccessful() =>
              result.flashing("request-success" -> "Petición guardada")
            case RequestJoinManager.RequestProcessError(reason) =>
              result.flashing(
                "request-error" -> s"Error al guardar: $reason"
              )

          }

    }
  def acceptRequest(requestID: UUID): Action[AnyContent] =
    silhouette.SecuredAction.async {
      implicit request: SecuredRequest[EnvType, AnyContent] =>
        val result = Redirect(routes.TeamManagerController.showAllTeams())

        requestManager
          .ask[RequestJoinManager.AcceptRequestResponse](ref =>
            RequestJoinManager.AcceptRequest(RequestJoinID(requestID), ref)
          )
          .map { _ =>
            result
          }

    }
  def removeRequest(requestID: UUID): Action[AnyContent] =
    silhouette.SecuredAction.async {
      implicit request: SecuredRequest[EnvType, AnyContent] =>
        val result = Redirect(routes.TeamManagerController.showAllTeams())

        requestManager
          .ask[RequestJoinManager.RemoveRequestResponse](ref =>
            RequestJoinManager
              .RemoveRequest(RequestJoinID(requestID), Some(ref))
          )
          .map { _ =>
            result
          }

    }

}
