package controllers
import com.mohiva.play.silhouette.api.actions.SecuredRequest
import javax.inject._
import play.api.mvc._
import play.api.i18n.I18nSupport

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ReplayMatchController @Inject()(scc: SilhouetteControllerComponents
                                     )(
                                       implicit
                                       assets: AssetsFinder,
                                       ex: ExecutionContext
                                     )extends   AbstractAuthController(scc) with I18nSupport {

  def addReplayToMatch(tournamentID: Long, matchID: Long): Action[AnyContent] = silhouette.SecuredAction.async { implicit request: SecuredRequest[EnvType, AnyContent] =>
      Future.successful(Ok(""))

  }

}
