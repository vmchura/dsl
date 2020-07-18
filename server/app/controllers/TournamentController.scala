package controllers

import com.mohiva.play.silhouette.api.actions.SecuredRequest
import forms.CreateTournamentForm
import javax.inject._
import jobs.{CannontAccessChallongeTournament, CannotAccesDiscordGuild, TournamentBuilder}
import play.api.mvc._
import play.api.i18n.I18nSupport
import shared.utils.BasicComparableByLabel

import scala.concurrent.{ExecutionContext, Future}
import upickle.default._
@Singleton
class TournamentController @Inject()(scc: SilhouetteControllerComponents,
                                     createTournamentView: views.html.createtournament,
                                     matchpairs: views.html.matchpairs,
                                     showmatches: views.html.matches,
                                     tournamentBuilder: TournamentBuilder
                           ) (
                             implicit
                             assets: AssetsFinder,
                             ex: ExecutionContext
                           )extends   AbstractAuthController(scc) with I18nSupport {

  def view(): Action[AnyContent] = silhouette.SecuredAction(WithAdmin()).async { implicit request: SecuredRequest[EnvType, AnyContent] =>
    Future.successful(Ok(createTournamentView(CreateTournamentForm.form)))
  }
  def post(): Action[AnyContent] = silhouette.SecuredAction(WithAdmin()).async { implicit request: SecuredRequest[EnvType, AnyContent] =>
    CreateTournamentForm.form.bindFromRequest.fold(
      form => Future.successful(BadRequest(createTournamentView(form))),
      data => {
        tournamentBuilder.buildTournament(data.discordGuildID,data.challongeID).map{
          case Left(error: CannontAccessChallongeTournament) =>
            logger.error(error.toString)
            BadRequest(createTournamentView(CreateTournamentForm.form.
              fill(CreateTournamentForm.Data(data.discordGuildID,""))))
          case Left(error: CannotAccesDiscordGuild) =>
            logger.error(error.toString)
            BadRequest(createTournamentView(CreateTournamentForm.form.
              fill(CreateTournamentForm.Data("",data.challongeID))))
          case Left(error) =>
            logger.error(error.toString)
            BadRequest(createTournamentView(CreateTournamentForm.form))
          case Right(tournament) => Redirect(routes.TournamentController.showparticipantscorrelation(tournament.challongeID))

        }

      }
    )
  }

  def showparticipantscorrelation(tournamentID: Long): Action[AnyContent] = silhouette.SecuredAction.async{ implicit request: SecuredRequest[EnvType, AnyContent] =>
    tournamentBuilder.getParticipantsUsers(tournamentID).map{
      case Left(error) =>Ok(s"error: ${error.toString}")
      case Right((tournament,participants, discordusers)) => Ok(matchpairs(tournament,
        participants.map(p => BasicComparableByLabel(p.chaname,write(p.participantPK))),
        discordusers.map(p => BasicComparableByLabel(p.userName, write(p.discordID)))))
    }

  }
  def showMatches(challongeTournamentID: Long): Action[AnyContent] = silhouette.SecuredAction.async { implicit request: SecuredRequest[EnvType, AnyContent] =>

    tournamentBuilder.getMatches(challongeTournamentID,Some(request.identity)).map{
      case Left(error) =>Ok(s"error: ${error.toString}")
      case Right(matches) => Ok(showmatches(matches))
    }
  }


}
