package controllers

import com.mohiva.play.silhouette.api.actions.UserAwareRequest

import javax.inject._
import models.daos.UserHistoryDAO
import models.{TournamentSeasonFilled, TournamentSeriesFilled}
import models.services.{SideBarMenuService, TournamentSeriesService}
import play.api.i18n.I18nSupport
import play.api.mvc._
import shared.models.DiscordID

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class StaticsController @Inject() (
    scc: SilhouetteControllerComponents,
    tournamentView: views.html.tournament,
    tournamentSeriesService: TournamentSeriesService,
    userHistoryDAO: UserHistoryDAO,
    sideBarMenuService: SideBarMenuService
)(implicit
    assets: AssetsFinder,
    ex: ExecutionContext
) extends AbstractAuthController(scc)
    with I18nSupport {

  def view(): Action[AnyContent] =
    silhouette.UserAwareAction.async {
      implicit request: UserAwareRequest[EnvType, AnyContent] =>
        sideBarMenuService.buildUserAwareSideBar().flatMap {
          case (menues, discriminator) =>
            implicit val menuesImplicit = menues
            implicit val socialProviders = socialProviderRegistry
            for {
              tournamentSeries <- tournamentSeriesService.allSeries()
              users <-
                Future
                  .traverse(
                    tournamentSeries.flatMap(
                      _.seasons
                        .flatMap(_.winners)
                        .map(_._2)
                        .map(DiscordID.apply)
                    )
                  )(
                    userHistoryDAO.load
                  )
                  .map(_.flatten)
            } yield {
              Ok(
                tournamentView(
                  request.identity,
                  discriminator,
                  tournamentSeries.map(x => {
                    TournamentSeriesFilled(
                      x.id,
                      x.name,
                      x.seasons
                        .sortBy(_.season)
                        .map(y => {
                          val newWinners = y.winners.flatMap { z =>
                            val (order, userID) = z
                            users.find(_.discordID.id.equals(userID)).map { u =>
                              (order, u)
                            }

                          }
                          TournamentSeasonFilled(
                            y.challongeID,
                            y.season,
                            newWinners
                          )
                        })
                    )
                  })
                )
              )

            }
        }

    }

}
