package models.services


import controllers.{WithAdmin, routes}
import javax.inject.Inject
import models.{MenuActionDefined, MenuGroup, Tournament, User}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SideBarMenuService @Inject()(tournamentService: TournamentService) {

  def buildSideBar(user: Option[User]): Future[Seq[MenuGroup]] = {
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


      List(misTorneos,Some(todos),admin).flatten

    }
  }

}
