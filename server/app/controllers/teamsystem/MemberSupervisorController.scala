package controllers.teamsystem

import com.mohiva.play.silhouette.api.actions.{SecuredRequest, UserAwareRequest}
import controllers.{
  AbstractAuthController,
  AssetsFinder,
  SilhouetteControllerComponents
}
import models.daos.DiscordPlayerLoggedDAO

import javax.inject._
import models.services.SideBarMenuService
import modules.teamsystem.MemberQueryForm
import play.api.mvc._
import play.api.i18n.I18nSupport
import play.api.libs.json.{JsString, Json}
import shared.models.DiscordPlayerLogged

import scala.concurrent.{ExecutionContext, Future}
import upickle.default._
class MemberSupervisorController @Inject() (
    scc: SilhouetteControllerComponents,
    discordPlayerLoggedDAO: DiscordPlayerLoggedDAO
)(implicit
    assets: AssetsFinder,
    ex: ExecutionContext
) extends AbstractAuthController(scc)
    with I18nSupport {
  private def convertResponse(
      response: Either[String, Seq[DiscordPlayerLogged]]
  ) = Ok(JsString(write(response)))

  def findMembers(): Action[AnyContent] =
    silhouette.SecuredAction.async {
      implicit request: SecuredRequest[EnvType, AnyContent] =>
        MemberQueryForm.memberQuery
          .bindFromRequest()
          .fold(
            _ =>
              Future.successful(convertResponse(Left("query not well formed"))),
            query => {
              discordPlayerLoggedDAO
                .find(query)
                .map { res =>
                  convertResponse(Right(res))
                }
                .fallbackTo {
                  Future.successful(convertResponse(Left("Error on db")))
                }
            }
          )
    }

}
