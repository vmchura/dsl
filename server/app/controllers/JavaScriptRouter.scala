package controllers

import javax.inject.Inject
import play.api.i18n.I18nSupport
import play.api.mvc._
import play.api.routing._

class JavaScriptRouter @Inject() (components: ControllerComponents)
    extends AbstractController(components)
    with I18nSupport {
  def javascriptRoutes: Action[AnyContent] =
    Action { implicit request =>
      Ok(
        JavaScriptReverseRouter("jsRoutes")(
          routes.javascript.ReplayMatchController.parseReplay,
          teamsystem.routes.javascript.MemberSupervisorController.findMembers,
          teamsystem.routes.javascript.TeamReplayController.submitTeamReplay
        )
      )
    }
}
