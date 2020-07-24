package controllers
import javax.inject._
import jobs.ReplayPusher
import play.api.mvc._
import play.api.i18n.I18nSupport
import play.api.libs.Files

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ReplayMatchController @Inject()(scc: SilhouetteControllerComponents,
                                      replayPusher: ReplayPusher
                                     )(
                                       implicit
                                       assets: AssetsFinder,
                                       ex: ExecutionContext
                                     )extends   AbstractAuthController(scc) with I18nSupport {

  def addReplayToMatch(tournamentID: Long, matchID: Long): Action[MultipartFormData[Files.TemporaryFile]] = silhouette.SecuredAction.async(parse.multipartFormData) { implicit request =>
    val result = Redirect(routes.TournamentController.showMatches(tournamentID))
    request.body.file("replay_file").fold(Future.successful(result.flashing("error" -> "quieres hackearme?"))){ replay_file =>
      replayPusher.pushReplay(tournamentID,matchID,replay_file.ref.toFile, request.identity, replay_file.filename).map {
        case Left(error) => result.flashing("error" -> error.toString)
        case Right(_) => result.flashing("success" -> s"${replay_file.filename} guardado!")
      }

    }

  }


}
