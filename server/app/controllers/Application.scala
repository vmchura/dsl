package controllers

import com.mohiva.play.silhouette.api.LogoutEvent
import com.mohiva.play.silhouette.api.actions.{SecuredRequest, UserAwareRequest}
import models.{PostDataFromUsage, PrismarisDAOTestTemp}
import models.daos.DiscordPlayerLoggedDAO

import javax.inject._
import models.services.SideBarMenuService
import play.api.mvc._
import play.api.i18n.I18nSupport
import play.api.libs.json.Json
import shared.models.DiscordID
import utils.route.Calls

import java.util.UUID
import scala.concurrent.ExecutionContext

@Singleton
class Application @Inject() (
    scc: SilhouetteControllerComponents,
    welcomeauthenticated: views.html.welcomeauthenticated,
    sideBarMenuService: SideBarMenuService,
    prismarisDAOTestTemp: PrismarisDAOTestTemp
)(implicit
    assets: AssetsFinder,
    ex: ExecutionContext
) extends AbstractAuthController(scc)
    with I18nSupport {

  def index(): Action[AnyContent] =
    silhouette.UserAwareAction.async {
      implicit request: UserAwareRequest[EnvType, AnyContent] =>
        sideBarMenuService.buildUserAwareSideBar().map {
          case (menues, discriminator) =>
            Redirect(routes.StaticsController.view())
        }
    }

  def welcomeAuthenticated(): Action[AnyContent] =
    silhouette.SecuredAction.async {
      implicit request: SecuredRequest[EnvType, AnyContent] =>
        sideBarMenuService.buildLoggedSideBar().map {
          case (menues, discriminator) =>
            implicit val menuesImplicit = menues
            implicit val socialProviders = socialProviderRegistry
            Ok(
              welcomeauthenticated(
                request.identity,
                discriminator
              )
            )

        }
    }
  def signOut(): Action[AnyContent] = {
    silhouette.SecuredAction.async {
      implicit request: SecuredRequest[EnvType, AnyContent] =>
        val result = Redirect(Calls.home)
        eventBus.publish(LogoutEvent(request.identity, request))
        authenticatorService.discard(request.authenticator, result)

    }

  }

  def helloPrismaris(clientID: UUID, action: String): Action[AnyContent] =
    Action.async { implicit request =>
      prismarisDAOTestTemp.addLog(PostDataFromUsage(clientID, action)).map {
        resp =>
          Ok(Json.toJson(resp))
      }

    }
}
