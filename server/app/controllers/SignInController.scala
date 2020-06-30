package controllers
import javax.inject.Inject
import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.api.util.Clock
import com.mohiva.play.silhouette.impl.providers._

import scala.language.postfixOps
import play.api.Configuration
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent}
import utils.auth.DefaultEnv

import scala.concurrent.{ExecutionContext, Future}

class SignInController @Inject() (
                                   scc: SilhouetteControllerComponents,
                                 )(
                                   implicit
                                   assets: AssetsFinder,
                                   ex: ExecutionContext
                                 ) extends  AbstractAuthController(scc)with I18nSupport {

  def view: Action[AnyContent] = silhouette.UnsecuredAction.async { implicit request =>
    Future.successful(Ok(views.html.signIn(socialProviderRegistry)))
  }
}
