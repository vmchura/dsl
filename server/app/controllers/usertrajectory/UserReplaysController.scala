package controllers.usertrajectory
import com.mohiva.play.silhouette.api.actions.UserAwareRequest
import controllers.{
  AbstractAuthController,
  AssetsFinder,
  SilhouetteControllerComponents
}
import models.daos.usertrajectory.ReplayRecordResumenDAO
import models.services.SideBarMenuService
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent}

import javax.inject._
import scala.concurrent.ExecutionContext
@Singleton
class UserReplaysController @Inject() (
    scc: SilhouetteControllerComponents,
    sideBarMenuService: SideBarMenuService,
    replayRecordResumenDAO: ReplayRecordResumenDAO,
    replaysUserViewer: views.html.usertrajectory
)(implicit
    assets: AssetsFinder,
    ex: ExecutionContext
) extends AbstractAuthController(scc)
    with I18nSupport {
  def loadReplaysByUser(discordUserID: String): Action[AnyContent] =
    silhouette.UserAwareAction.async {
      implicit request: UserAwareRequest[EnvType, AnyContent] =>
        sideBarMenuService.buildUserAwareSideBar().flatMap { implicit menues =>
          replayRecordResumenDAO
            .load(ReplayRecordResumenDAO.ByPlayer(discordUserID))
            .map { records =>
              Ok(
                replaysUserViewer(
                  request.identity,
                  records,
                  socialProviderRegistry
                )
              )
            }
        }
    }
}
