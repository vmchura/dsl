package controllers.teamsystem
import akka.actor.typed.ActorRef
import com.mohiva.play.silhouette.api.actions.SecuredRequest
import controllers.{
  AbstractAuthController,
  AssetsFinder,
  SilhouetteControllerComponents
}
import models.daos.DiscordPlayerLoggedDAO
import models.daos.teamsystem.{InvitationDAO, RequestDAO, TeamDAO}

import javax.inject._
import models.services.SideBarMenuService
import models.teamsystem.{
  InvitationID,
  InvitationWithUsers,
  RequestJoinWithUsers,
  TeamID,
  TeamWithUsers
}
import modules.teamsystem.{
  InvitationManager,
  MemberQueryForm,
  MemberSupervisor,
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
    invitationDAO: InvitationDAO,
    teamCreator: ActorRef[CreationCommand],
    teamManager: ActorRef[TeamManager.TeamManagerCommand],
    invitationManager: ActorRef[InvitationManager.InvitationCommand],
    requestDAO: RequestDAO
)(implicit
    assets: AssetsFinder,
    ex: ExecutionContext,
    discordPlayerLoggedDAO: DiscordPlayerLoggedDAO,
    scheduler: akka.actor.typed.Scheduler,
    teamDAO: TeamDAO
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
  def removeInvitation(invitationID: UUID): Action[AnyContent] =
    silhouette.SecuredAction.async {
      implicit request: SecuredRequest[EnvType, AnyContent] =>
        val result = Redirect(routes.TeamManagerController.showMyTeams())

        invitationManager
          .ask[InvitationManager.InvitationManagerResponse](ref =>
            InvitationManager.RemoveInvitation(InvitationID(invitationID), ref)
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
              .map(_.flatten)
              .flatMap(teams =>
                Future.traverse(teams)(team => {
                  if (team.principal.discordID == userDiscordID) {
                    requestDAO
                      .requestsToTeam(team.teamID)
                      .map(requests => (team, requests))
                  } else {
                    Future.successful((team, Nil))
                  }
                })
              )
              .flatMap { teams =>
                Future.traverse(teams) {
                  case (team, requests) =>
                    Future
                      .traverse(requests) { req =>
                        RequestJoinWithUsers(req)
                      }
                      .map(requestsOpt => (team, requestsOpt.flatten))

                }

              }

          val invitationsWithusers = invitationDAO
            .invitationsToUser(userDiscordID)
            .flatMap(invitations =>
              Future.traverse(invitations)(InvitationWithUsers.apply)
            )

          teamsWithUsers.zip(invitationsWithusers).map {
            case (teams, invitations) =>
              Ok(
                views.html.teamsystem
                  .showmyteams(
                    request.identity,
                    userDiscordID,
                    teams,
                    invitations.flatten,
                    TeamInvitationForm.teamInvitation,
                    TeamCreationForm.teamCreation,
                    MemberQueryForm.memberQuery,
                    socialProviderRegistry
                  )
              )
          }

        }
    }
  def showAllTeams(): Action[AnyContent] =
    silhouette.UserAwareAction.async { implicit request =>
      sideBarMenuService.buildUserAwareSideBar().flatMap { implicit menues =>
        val teamsFut = teamDAO.loadTeams()
        val teamsWithUsers =
          teamsFut
            .flatMap(teams => Future.traverse(teams)(TeamWithUsers.apply))

        val userCanRequestFut = request.identity
          .fold(Future.successful(false))(id =>
            teamDAO.isOfficial(DiscordID(id.loginInfo.providerKey)).map(!_)
          )

        teamsWithUsers.zip(userCanRequestFut).map {
          case (teams, userCanRequest) =>
            Ok(
              views.html.teamsystem
                .showteams(
                  request.identity,
                  teams.flatten,
                  userCanRequest,
                  socialProviderRegistry
                )
            )
        }

      }
    }
}
