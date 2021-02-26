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
import play.api.mvc.{Action, AnyContent, MultipartFormData, Result}
import shared.models.{DiscordID, ReplayTeamID}
import akka.actor.typed.scaladsl.AskPattern._
import akka.util.Timeout
import models.Smurf
import shared.models.teamsystem.{
  ReplaySaved,
  SmurfToVerify,
  SpecificTeamReplayResponse,
  TeamReplayError,
  TeamReplayResponse
}

import java.util.UUID
import javax.inject.Inject
import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps
import upickle.default._
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

  def buildResponse(
      futResponse: Future[SpecificTeamReplayResponse]
  ): Future[Result] = {
    futResponse
      .map { response =>
        TeamReplayResponse(response)
      }
      .recover { error =>
        TeamReplayResponse(TeamReplayError(error.getMessage))
      }
      .map { response =>
        Ok(write(response))
      }
  }

  def submitTeamReplay(): Action[MultipartFormData[Files.TemporaryFile]] =
    silhouette.SecuredAction.async(parse.multipartFormData) {
      implicit request =>
        val discordID = DiscordID(request.identity.loginInfo.providerKey)
        val futResponse = teamDAO
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
                        TeamReplayError(reason)
                      case TeamReplaySubmit.ReplaySavedResponse() =>
                        ReplaySaved()
                      case TeamReplaySubmit
                            .SmurfMustBeSelectedResponse(
                              replayTeamID,
                              oneVsOne
                            ) =>
                        SmurfToVerify(replayTeamID, oneVsOne)

                    }
                case None =>
                  Future.successful(
                    TeamReplayError("No replay acquired")
                  )
              }

            case None =>
              Future.successful(
                TeamReplayError("You are not official member of a team")
              )
          }

        buildResponse(futResponse)

    }

  def selectSmurf(smurf: String, replayTeamID: UUID): Action[AnyContent] =
    silhouette.SecuredAction.async { implicit request =>
      val futResponse = submitActor
        .ask[TeamReplaySubmit.Response](ref =>
          TeamReplayManager
            .SmurfSelected(
              replayTeamID = ReplayTeamID(replayTeamID),
              smurf = Smurf(smurf),
              replyTo = ref
            )
        )
        .map {
          case TeamReplaySubmit.SubmitError(reason) =>
            TeamReplayError(reason)
          case TeamReplaySubmit.ReplaySavedResponse() =>
            ReplaySaved()
          case _ => TeamReplayError("Unexpected response")
        }
      buildResponse(futResponse)
    }
}
