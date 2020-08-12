package controllers

import com.mohiva.play.silhouette.api.actions.SecuredRequest
import javax.inject._
import models.services.SideBarMenuService
import play.api.mvc._
import play.api.i18n.I18nSupport

import scala.concurrent.ExecutionContext

@Singleton
class Application @Inject()(scc: SilhouetteControllerComponents,
                            welcomeauthenticated: views.html.welcomeauthenticated,
                            index: views.html.index,
                           sideBarMenuService: SideBarMenuService
) (
  implicit
  assets: AssetsFinder,
  ex: ExecutionContext
)extends   AbstractAuthController(scc) with I18nSupport {

  def index(): Action[AnyContent] = silhouette.UserAwareAction.async { implicit request =>
    sideBarMenuService.buildSideBar(request.identity).map{ menues =>
      Ok(index(request.identity, menues,socialProviderRegistry))
    }
    /*
    request.identity match {
      case Some(_) => Redirect(routes.Application.welcomeAuthenticated())
      case None => Redirect(routes.SignInController.view())
    }

     */
  }
  def welcomeAuthenticated(): Action[AnyContent] = silhouette.SecuredAction.async { implicit request: SecuredRequest[EnvType, AnyContent] =>
    sideBarMenuService.buildSideBar(Some(request.identity)).map{ menues =>
      Ok(welcomeauthenticated(request.identity, menues,socialProviderRegistry))
    }

  }
}
