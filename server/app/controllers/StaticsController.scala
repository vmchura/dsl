package controllers

import com.mohiva.play.silhouette.api.actions.UserAwareRequest
import javax.inject._
import models.services.SideBarMenuService
import play.api.i18n.I18nSupport
import play.api.mvc._

import scala.concurrent.ExecutionContext

@Singleton
class StaticsController @Inject() (
    scc: SilhouetteControllerComponents,
    tournamentView: views.html.tournament,
    sideBarMenuService: SideBarMenuService
)(implicit
    assets: AssetsFinder,
    ex: ExecutionContext
) extends AbstractAuthController(scc)
    with I18nSupport {

  def view(): Action[AnyContent] =
    silhouette.UserAwareAction.async {
      implicit request: UserAwareRequest[EnvType, AnyContent] =>
        sideBarMenuService.buildUserAwareSideBar().map { implicit menues =>
          Ok(
            tournamentView(
              socialProviderRegistry
            )
          )
        }

    }

}
