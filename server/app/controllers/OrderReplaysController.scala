package controllers

import java.util.UUID

import forms.OrderGamesForm
import javax.inject._
import models.daos.ReplayMatchDAO
import models.services.{SideBarMenuService, TournamentService}
import play.api.i18n.I18nSupport

import scala.concurrent.{ExecutionContext, Future}
import forms.OrderGamesForm.Data
import jobs.ReplayService
import play.api.mvc.{Action, AnyContent}
@Singleton
class OrderReplaysController @Inject()(scc: SilhouetteControllerComponents,
                                      replayMatchDAO: ReplayMatchDAO,
                                      sideBarMenuService: SideBarMenuService,
                                      orderreplays: views.html.listreplaysbracket,
                                      tournamentService: TournamentService,
                                      replayService: ReplayService
                                     )(
                                       implicit
                                       assets: AssetsFinder,
                                       ex: ExecutionContext
                                     )extends   AbstractAuthController(scc) with I18nSupport {

  def view(tournamentID: Long,matchID: Long): Action[AnyContent] = silhouette.SecuredAction(WithAdmin()).async { implicit request =>
    sideBarMenuService.buildLoggedSideBar().flatMap { implicit menues =>
      for{
        tournamentOpt <- tournamentService.findTournament(tournamentID)
        tournament <- tournamentOpt match {
          case Some(value) => Future.successful(value)
          case _ => Future.failed(new IllegalArgumentException("No tournament found"))
        }
        replays <- replayMatchDAO.loadAllByMatch(tournamentID, matchID)
      }yield{
        Ok(orderreplays(request.identity,tournament.tournamentName, tournamentID, matchID,replays, OrderGamesForm.form.fill(Data(3,replays.filter(_.enabled).map(_.replayID).toList)),socialProviderRegistry))
      }

    }

  }
  def submit(tournamentID: Long,matchID: Long): Action[AnyContent] = silhouette.SecuredAction(WithAdmin()).async { implicit request =>

      OrderGamesForm.form.bindFromRequest.fold(
        _ => Future.successful(Redirect(routes.OrderReplaysController.view(tournamentID, matchID))),
        data => {
          replayService.createFoldersAntiSpoilers(tournamentID, matchID, data.bof, data.replayID)
          Future.successful(Ok(""))
        })

  }
}
