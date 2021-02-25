package controllers.teamsystem

import akka.actor.typed.ActorRef
import controllers.{
  AbstractAuthController,
  AssetsFinder,
  SilhouetteControllerComponents
}
import models.daos.teamsystem.TeamDAO
import modules.teamsystem.{TeamReplayManager, TeamReplaySubmit}
import play.api.i18n.I18nSupport
import play.api.libs.Files
import play.api.mvc.{Action, MultipartFormData}
import shared.models.DiscordID
import akka.actor.typed.scaladsl.AskPattern._
import akka.util.Timeout

import javax.inject.Inject
import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

class TeamReplayController @Inject() (
    scc: SilhouetteControllerComponents,
    submitActor: ActorRef[TeamReplayManager.Command]
)(implicit
    assets: AssetsFinder,
    ex: ExecutionContext,
    scheduler: akka.actor.typed.Scheduler,
    teamDAO: TeamDAO
) extends AbstractAuthController(scc)
    with I18nSupport {
  implicit val timeOut: Timeout = 10 seconds

  def submitTeamReplay(): Action[MultipartFormData[Files.TemporaryFile]] =
    silhouette.SecuredAction.async(parse.multipartFormData) {
      implicit request =>
        val discordID = DiscordID(request.identity.loginInfo.providerKey)
        teamDAO
          .teamsOf(discordID)
          .map(_.find(_.isOfficial(discordID)))
          .flatMap {
            case Some(team) =>
              request.body.file("replay_file") match {
                case Some(file) =>
                  submitActor
                    .ask[TeamReplaySubmit.Response](ref =>
                      TeamReplayManager
                        .Submit(discordID, team.teamID, file.ref.toFile, ref)
                    )
                    .map {
                      case TeamReplaySubmit.SubmitError(reason) =>
                        Ok(reason)
                      case TeamReplaySubmit.ReplaySavedResponse() =>
                        Ok("replay saved")
                      case TeamReplaySubmit
                            .SmurfMustBeSelectedResponse(_, _) =>
                        Ok("needs confirmation")
                    }
                case None => Future.successful(Ok("No replay acquired"))
              }

            case None =>
              Future.successful(Ok("You are not official member of a team"))
          }

    }
}
