package controllers

import java.util.UUID

import javax.inject.Inject
import models.TournamentMenu
import models.daos.UserSmurfDAO
import models.services.{SmurfService, TournamentService}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent}

import scala.concurrent.ExecutionContext

class SmurfController @Inject()(scc: SilhouetteControllerComponents,
                               smurfsToCheck: views.html.smurfstoverify,
                               tournamentService: TournamentService,
                               userSmurfDAO: UserSmurfDAO,
                               smurfService: SmurfService
                               ) (
                                 implicit
                                 assets: AssetsFinder,
                                 ex: ExecutionContext
                               )extends   AbstractAuthController(scc) with I18nSupport {

  def view(): Action[AnyContent] = silhouette.SecuredAction(WithAdmin()).async { implicit request =>
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
  def accept(discordUserID: String,matchID: UUID): Action[AnyContent] = silhouette.SecuredAction(WithAdmin()).async { implicit request =>
    for {
      accepted <- smurfService.acceptSmurf(discordUserID, matchID)

    } yield {

      val result = Redirect(routes.SmurfController.view())

      if(accepted)
        result.flashing("success" -> "Relación aceptada y guardad")
      else
        result.flashing("error" -> "ERROR EN GUARDAR LA RELACION")
    }
  }

}
