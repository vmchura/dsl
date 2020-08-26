package controllers
import javax.inject.Inject
import models.services.SideBarMenuService

import scala.language.postfixOps
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent}

import scala.concurrent.{ExecutionContext, Future}

class SignInController @Inject() (
                                   scc: SilhouetteControllerComponents,
                                   signIn: views.html.signIn,
                                    sideBarMenuService: SideBarMenuService
                                 )(
                                   implicit
                                   assets: AssetsFinder,
                                   ex: ExecutionContext
                                 ) extends  AbstractAuthController(scc)with I18nSupport {

  def view(): Action[AnyContent] = silhouette.UnsecuredAction.async { implicit request =>
    sideBarMenuService.buildGuestSideBar().map{ implicit menues =>
      Ok(signIn(socialProviderRegistry))
    }
  }
}
