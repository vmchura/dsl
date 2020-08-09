package models.services

import java.util.UUID

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.impl.providers.CommonSocialProfile
import controllers.routes
import javax.inject.Inject
import models.{MenuActionDefined, MenuGroup, User}
import models.daos.UserDAO

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SideBarMenuService @Inject()(tournamentService: TournamentService) {

  def buildSideBar(user: Option[User]): Future[Seq[MenuGroup]] = {
    for{
      torneos <- tournamentService.findAllTournaments()
    }yield{
      val misTorneos = MenuGroup("Mis Torneos",torneos.map(torneo =>
        MenuActionDefined(torneo.tournamentName,
          routes.TournamentController.showMatchesToUploadReplay(torneo.challongeID).url
        )))
      val todos = MenuGroup("Torneos",torneos.map(torneo =>
        MenuActionDefined(torneo.tournamentName,
          routes.TournamentController.showMatches(torneo.challongeID).url
        )))


      List(misTorneos,todos)

    }
  }

}
