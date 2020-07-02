package controllers

import com.mohiva.play.silhouette.api.actions.SecuredRequest
import javax.inject._
import play.api.mvc._
import play.api.i18n.I18nSupport

import scala.concurrent.ExecutionContext

@Singleton
class Application @Inject()(scc: SilhouetteControllerComponents,
                            indexpage: views.html.index,
                            welcomeauthenticated: views.html.welcomeauthenticated
) (
  implicit
  assets: AssetsFinder,
  ex: ExecutionContext
)extends   AbstractAuthController(scc) with I18nSupport {

  def index: Action[AnyContent] = Action { implicit request =>
    Ok(indexpage())
  }
  def welcomeAuthenticated: Action[AnyContent] = silhouette.SecuredAction { implicit request: SecuredRequest[EnvType, AnyContent] =>
    Ok(welcomeauthenticated(request.identity))
  }
}
