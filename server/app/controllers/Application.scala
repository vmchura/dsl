package controllers

import com.mohiva.play.silhouette.api.actions.SecuredRequest
import javax.inject._
import models.TournamentMenu
import models.services.TournamentService
import play.api.mvc._
import play.api.i18n.I18nSupport

import scala.concurrent.ExecutionContext

@Singleton
class Application @Inject()(scc: SilhouetteControllerComponents,
                            indexpage: views.html.index,
                            welcomeauthenticated: views.html.welcomeauthenticated,
                            tournamentService: TournamentService
) (
  implicit
  assets: AssetsFinder,
  ex: ExecutionContext
)extends   AbstractAuthController(scc) with I18nSupport {

  def index(): Action[AnyContent] = Action { implicit request =>
    Ok(indexpage())
  }
  def welcomeAuthenticated(): Action[AnyContent] = silhouette.SecuredAction.async { implicit request: SecuredRequest[EnvType, AnyContent] =>
    tournamentService.findAllTournaments().map{ torneos =>
      Ok(welcomeauthenticated(request.identity, torneos.map(torneo =>
        TournamentMenu(torneo.tournamentName,
          routes.TournamentController.showMatches(torneo.challongeID).url
          ))))
    }

  }
}
