package models.services


import com.mohiva.play.silhouette.api.actions.{SecuredRequest, UserAwareRequest}
import controllers.{WithAdmin, routes}
import javax.inject.Inject
import models.{MenuActionDefined, MenuGroup, Tournament, User}
import play.api.mvc.Request
import utils.auth.DefaultEnv

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SideBarMenuService @Inject()(tournamentService: TournamentService) {

  private def buildSideBar(user: Option[User]): Future[Seq[MenuGroup]] = {
    for{
      torneos <- tournamentService.findAllTournaments()
      registered <- user.fold(Future.successful(Seq.empty[Tournament]))(u => tournamentService.findAllTournamentsByPlayer(u.loginInfo.providerKey))
    }yield{
      val misTorneos = user.map(_ =>  MenuGroup("Mis Partidas",registered.map(torneo =>
        MenuActionDefined(torneo.tournamentName,
          routes.TournamentController.showMatchesToUploadReplay(torneo.challongeID).url
        ))))

      val todos = MenuGroup("Torneos",torneos.map(torneo =>
        MenuActionDefined(torneo.tournamentName,
          routes.TournamentController.showMatches(torneo.challongeID).url
        )))

      val admin = user.flatMap(u => if(WithAdmin.isModeradorID(u.loginInfo.providerKey)) Some(MenuGroup("Admin",List(MenuActionDefined("Smurfs",routes.SmurfController.view().url)))) else None)

      val myStats = user.map(_ =>
        MenuGroup("Mis stats", List(MenuActionDefined("Mis Stats",routes.UserStatsAPI.viewMyStats().url)))
      )
      List(misTorneos,Some(todos),admin,myStats).flatten
    }
  }


  def buildLoggedSideBar()(implicit request: SecuredRequest[DefaultEnv,_]): Future[Seq[MenuGroup]] = buildSideBar(Some(request.identity))
  def buildUserAwareSideBar()(implicit request: UserAwareRequest[DefaultEnv,_]): Future[Seq[MenuGroup]] = buildSideBar(request.identity)
  def buildGuestSideBar(): Future[Seq[MenuGroup]] = buildSideBar(None)



}
