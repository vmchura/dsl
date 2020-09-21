package controllers
import java.util.UUID

import javax.inject._
import jobs.{CannotSaveResultMatch, CannotSmurf, ReplayService}
import models.{Match, MatchPK, MatchResult, MatchSmurf}
import models.daos.{MatchResultDAO, ReplayMatchDAO, UserSmurfDAO}
import models.services.{ParseReplayFileService, SideBarMenuService, TournamentService}
import play.api.mvc._
import play.api.i18n.I18nSupport
import play.api.libs.Files
import play.api.libs.json.Json
import shared.models.ActionByReplay
import shared.models.ActionBySmurf._
import upickle.default._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}
@Singleton
class OrderReplaysController @Inject()(scc: SilhouetteControllerComponents,
                                      replayService: ReplayService,
                                      matchResultDAO: MatchResultDAO,
                                      replayMatchDAO: ReplayMatchDAO,
                                      sideBarMenuService: SideBarMenuService,
                                      orderreplays: views.html.listreplaysbracket,
                                      tournamentService: TournamentService
                                     )(
                                       implicit
                                       assets: AssetsFinder,
                                       ex: ExecutionContext
                                     )extends   AbstractAuthController(scc) with I18nSupport {

  def view(tournamentID: Long,matchID: Long) = silhouette.SecuredAction(WithAdmin()).async { implicit request =>
    sideBarMenuService.buildLoggedSideBar().flatMap { implicit menues =>
      for{
        tournamentOpt <- tournamentService.findTournament(tournamentID)
        tournament <- tournamentOpt match {
          case Some(value) => Future.successful(value)
          case _ => Future.failed(new IllegalArgumentException("No tournament found"))
        }
        replays <- replayMatchDAO.loadAllByMatch(tournamentID, matchID)
      }yield{

        Ok(orderreplays(request.identity,tournament.tournamentName,replays,socialProviderRegistry))
      }

    }

  }
}
