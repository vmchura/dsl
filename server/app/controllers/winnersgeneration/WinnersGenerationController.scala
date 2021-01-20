package controllers.winnersgeneration
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.ActorRef
import akka.util.Timeout
import com.mohiva.play.silhouette.api.actions.{SecuredRequest, UserAwareRequest}
import controllers.{
  AbstractAuthController,
  AssetsFinder,
  SilhouetteControllerComponents,
  WithAdmin
}
import forms.CreateTournamentForm

import javax.inject._
import jobs.{
  CannontAccessChallongeTournament,
  CannotAccesDiscordGuild,
  TournamentBuilder
}
import models.Tournament
import models.services.{SideBarMenuService, TournamentService}
import modules.winnersgeneration.WinnersForm
import modules.winnersgeneration.WinnersGathering.{
  Gather,
  GatheringFail,
  GatheringSucess,
  WinnersGatheringCommand,
  WinnersGatheringResponse
}
import play.api.mvc._
import play.api.i18n.I18nSupport
import shared.utils.BasicComparableByLabel

import scala.concurrent.{ExecutionContext, Future}
import upickle.default._

import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
@Singleton
class WinnersGenerationController @Inject() (
    scc: SilhouetteControllerComponents,
    informationGathe: ActorRef[WinnersGatheringCommand],
    sideBarMenuService: SideBarMenuService
)(implicit
    assets: AssetsFinder,
    ex: ExecutionContext,
    scheduler: akka.actor.typed.Scheduler
) extends AbstractAuthController(scc)
    with I18nSupport {

  implicit val timeOut: Timeout = Timeout(12 seconds)
  def view() =
    silhouette.SecuredAction(WithAdmin()).async {
      implicit request: SecuredRequest[EnvType, AnyContent] =>
        sideBarMenuService.buildLoggedSideBar.flatMap { implicit menues =>
          (informationGathe ? Gather).mapTo[WinnersGatheringResponse].map {
            case GatheringSucess(gatheredInformation) =>
              Ok(
                views.html.winnersgeneration.selectwinners(
                  request.identity,
                  WinnersForm.winnerForm,
                  gatheredInformation,
                  socialProviderRegistry
                )
              )
            case GatheringFail() => Ok("Failed")
          }

        }

    }
}
