package controllers.teamsystem

import com.mohiva.play.silhouette.api.actions.{SecuredRequest, UserAwareRequest}
import controllers.{
  AbstractAuthController,
  AssetsFinder,
  SilhouetteControllerComponents
}

import javax.inject._
import models.services.SideBarMenuService
import modules.teamsystem.MemberQueryForm
import play.api.mvc._
import play.api.i18n.I18nSupport
import play.api.libs.json.Json
import shared.models.DiscordPlayerLogged
import utils.route.Calls

import java.util.UUID
import scala.concurrent.ExecutionContext
import upickle.default._
class MemberSupervisorController @Inject() (
    scc: SilhouetteControllerComponents,
    sideBarMenuService: SideBarMenuService
)(implicit
    assets: AssetsFinder,
    ex: ExecutionContext
) extends AbstractAuthController(scc)
    with I18nSupport {
  /*def convertResponse(response: Either[String, Seq[DiscordPlayerLogged]]) =
    Ok(Json.obj("response" -> write(response)))
   */
  def findMembers() =
    Action { implicit request =>
      MemberQueryForm.memberQuery
        .bindFromRequest()
        .fold(
          _ => Ok(""),
          query => Ok("")
        )
    }

}
