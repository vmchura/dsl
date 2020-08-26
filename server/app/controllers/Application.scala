package controllers

import com.mohiva.play.silhouette.api.actions.{SecuredRequest, UserAwareRequest}
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

  def index(): Action[AnyContent] = silhouette.UserAwareAction.async { implicit request: UserAwareRequest[EnvType, AnyContent] =>
    sideBarMenuService.buildUserAwareSideBar().map{ implicit menues =>
      Ok(index(request.identity,socialProviderRegistry))
    }
  }

  def welcomeAuthenticated(): Action[AnyContent] = silhouette.SecuredAction.async { implicit request: SecuredRequest[EnvType, AnyContent] =>
    sideBarMenuService.buildLoggedSideBar().map{ implicit menues =>
      Ok(welcomeauthenticated(request.identity,socialProviderRegistry))
    }
  }
}
