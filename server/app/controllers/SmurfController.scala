package controllers

import javax.inject.Inject
import models.TournamentMenu
import models.daos.UserSmurfDAO
import models.services.TournamentService
import play.api.i18n.I18nSupport
import play.api.mvc.Result

import scala.concurrent.{ExecutionContext, Future}

class SmurfController @Inject()(scc: SilhouetteControllerComponents,
                               smurfsToCheck: views.html.smurfstoverify,
                               tournamentService: TournamentService,
                               userSmurfDAO: UserSmurfDAO
                               ) (
                                 implicit
                                 assets: AssetsFinder,
                                 ex: ExecutionContext
                               )extends   AbstractAuthController(scc) with I18nSupport {

  def view() = silhouette.SecuredAction(WithAdmin()).async { implicit request =>
    for {
      tournaments <- tournamentService.findAllTournaments()
      usersNotDefined <- userSmurfDAO.findUsersNotCompletelyDefined()

    } yield {
      Ok(smurfsToCheck(Some(request.identity), tournaments.map(torneo =>
        TournamentMenu(torneo.tournamentName,
          routes.TournamentController.showMatchesToUploadReplay(torneo.challongeID).url
        )), usersNotDefined))
    }
  }
}
