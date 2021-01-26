package controllers.teamsystem
import akka.actor.typed.ActorRef
import com.mohiva.play.silhouette.api.actions.SecuredRequest
import controllers.{
  AbstractAuthController,
  AssetsFinder,
  SilhouetteControllerComponents
}
import models.daos.DiscordPlayerLoggedDAO
import models.daos.teamsystem.TeamDAO

import javax.inject._
import models.services.SideBarMenuService
import models.teamsystem.{InvitationID, TeamID, TeamWithUsers}
import modules.teamsystem.{
  InvitationManager,
  TeamCreationForm,
  TeamCreator,
  TeamInvitationForm,
  TeamManager
}
import play.api.mvc._
import play.api.i18n.I18nSupport

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import akka.actor.typed.scaladsl.AskPattern._
import akka.util.Timeout
import modules.teamsystem.TeamCreator.CreationCommand
import shared.models.DiscordID

import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
class TeamManagerController @Inject() (
    scc: SilhouetteControllerComponents,
    sideBarMenuService: SideBarMenuService,
    teamDAO: TeamDAO,
    teamCreator: ActorRef[CreationCommand],
    teamManager: ActorRef[TeamManager.TeamManagerCommand],
    invitationManager: ActorRef[InvitationManager.InvitationCommand]
)(implicit
    assets: AssetsFinder,
    ex: ExecutionContext,
    discordPlayerLoggedDAO: DiscordPlayerLoggedDAO,
    scheduler: akka.actor.typed.Scheduler
) extends AbstractAuthController(scc)
    with I18nSupport {
  implicit val timeOut: Timeout = 10 seconds
  def doInvitation(teamID: UUID): Action[AnyContent] =
    silhouette.SecuredAction.async {
      implicit request: SecuredRequest[EnvType, AnyContent] =>
        TeamInvitationForm.teamInvitation.bindFromRequest.fold(
          _ =>
            Future.successful(
              Redirect(routes.TeamManagerController.showMyTeams())
            ),
          valueOnSuccess => {
            val result = Redirect(routes.TeamManagerController.showMyTeams())
            val from =
              DiscordID(request.authenticator.loginInfo.providerKey)
            invitationManager
              .ask[InvitationManager.InvitationManagerResponse](ref =>
                InvitationManager.Invite(
                  from,
                  valueOnSuccess.to,
                  TeamID(teamID),
                  valueOnSuccess.status,
                  ref
                )
              )
              .map {
                case InvitationManager.InvitationMade() =>
                  result.flashing("invitation-success" -> "Invitación guardada")
                case InvitationManager.InvitationError(reason) =>
                  result.flashing(
                    "invitation-error" -> s"Error al guardar: $reason"
                  )

              }
          }
        )

    }
  def acceptInvitation(invitationID: UUID): Action[AnyContent] =
    silhouette.SecuredAction.async {
      implicit request: SecuredRequest[EnvType, AnyContent] =>
        val result = Redirect(routes.TeamManagerController.showMyTeams())

        invitationManager
          .ask[InvitationManager.InvitationManagerResponse](ref =>
            InvitationManager.AcceptInvitation(InvitationID(invitationID), ref)
          )
          .map { _ =>
            result
          }

    }
  def quitTeam(teamID: UUID): Action[AnyContent] =
    silhouette.SecuredAction.async {
      implicit request: SecuredRequest[EnvType, AnyContent] =>
        val result = Redirect(routes.TeamManagerController.showMyTeams())
        val userDiscordID =
          DiscordID(request.authenticator.loginInfo.providerKey)

        teamManager
          .ask(ref =>
            TeamManager.RemoveUserFrom(userDiscordID, TeamID(teamID), ref)
          )
          .map { _ =>
            result
          }

    }

  def createTeam(): Action[AnyContent] =
    silhouette.SecuredAction.async {
      implicit request: SecuredRequest[EnvType, AnyContent] =>
        val result = Redirect(routes.TeamManagerController.showMyTeams())

        TeamCreationForm.teamCreation
          .bindFromRequest()
          .fold(
            _ => Future.successful(result),
            teamName => {
              val userDiscordID =
                DiscordID(request.authenticator.loginInfo.providerKey)

              teamCreator
                .ask(ref =>
                  TeamCreator.CreateTeam(ref, userDiscordID, teamName)
                )
                .map { _ =>
                  result
                }
            }
          )

    }
  def showMyTeams(): Action[AnyContent] =
    silhouette.SecuredAction.async {
      implicit request: SecuredRequest[EnvType, AnyContent] =>
        sideBarMenuService.buildLoggedSideBar().flatMap { implicit menues =>
          val userDiscordID =
            DiscordID(request.authenticator.loginInfo.providerKey)
          val teamsFut = teamDAO.teamsOf(userDiscordID)
          val teamsWithUsers =
            teamsFut
              .flatMap(teams => Future.traverse(teams)(TeamWithUsers.apply))
          teamsWithUsers.map { teams =>
            Ok(
              views.html.teamsystem
                .showmyteams(
                  request.identity,
                  userDiscordID,
                  teams.flatten,
                  TeamInvitationForm.teamInvitation,
                  TeamCreationForm.teamCreation,
                  socialProviderRegistry
                )
            )
          }

        }
    }
}
