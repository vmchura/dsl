package controllers

import com.mohiva.play.silhouette.api.actions.UserAwareRequest
import javax.inject._
import models.{DiscordUser, TournamentSeasonFilled, TournamentSeriesFilled}
import models.services.{
  SideBarMenuService,
  TournamentSeriesService,
  TournamentService
}
import play.api.i18n.I18nSupport
import play.api.mvc._

import scala.concurrent.ExecutionContext

@Singleton
class StaticsController @Inject() (
    scc: SilhouetteControllerComponents,
    tournamentView: views.html.tournament,
    tournamentSeriesService: TournamentSeriesService,
    tournamentService: TournamentService,
    sideBarMenuService: SideBarMenuService
)(implicit
    assets: AssetsFinder,
    ex: ExecutionContext
) extends AbstractAuthController(scc)
    with I18nSupport {

  def view(): Action[AnyContent] =
    silhouette.UserAwareAction.async {
      implicit request: UserAwareRequest[EnvType, AnyContent] =>
        sideBarMenuService.buildUserAwareSideBar().flatMap { implicit menues =>
          for {
            tournamentSeries <- tournamentSeriesService.allSeries()
          } yield {
            Ok(
              tournamentView(
                tournamentSeries.map(x =>
                  TournamentSeriesFilled(
                    x.id,
                    x.name,
                    x.seasons.map(y =>
                      TournamentSeasonFilled(
                        y.challongeID,
                        y.season,
                        List(
                          (1, DiscordUser("-", "Queen", None)),
                          (2, DiscordUser("-", "SSoma", None)),
                          (3, DiscordUser("-", "Flash", None))
                        )
                      )
                    )
                  )
                ),
                socialProviderRegistry
              )
            )

          }
        }

    }

}
