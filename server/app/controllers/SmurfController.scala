package controllers

import java.util.UUID

import javax.inject.Inject
import models.daos.UserSmurfDAO
import models.services.{SideBarMenuService, SmurfService, TournamentService}
import play.api.i18n.I18nSupport
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent}

import scala.concurrent.ExecutionContext

class SmurfController @Inject()(scc: SilhouetteControllerComponents,
                               smurfsToCheck: views.html.smurfstoverify,
                               tournamentService: TournamentService,
                               userSmurfDAO: UserSmurfDAO,
                               smurfService: SmurfService,
                               sideBarMenuService: SideBarMenuService
                               ) (
                                 implicit
                                 assets: AssetsFinder,
                                 ex: ExecutionContext
                               )extends   AbstractAuthController(scc) with I18nSupport {

  def view(): Action[AnyContent] = silhouette.SecuredAction(WithAdmin()).async { implicit request =>
    sideBarMenuService.buildLoggedSideBar().flatMap { implicit menues =>
      for {
        usersNotDefined <- userSmurfDAO.findUsersNotCompletelyDefined()

      } yield {

        Ok(smurfsToCheck(Some(request.identity), usersNotDefined, socialProviderRegistry))
      }
    }
  }
  def accept(discordUserID: String,matchID: UUID): Action[AnyContent] = silhouette.SecuredAction(WithAdmin()).async { implicit request =>
    for {
      accepted <- smurfService.acceptSmurf(discordUserID, matchID)

    } yield {

      val result = Redirect(routes.SmurfController.view())

      if(accepted)
        result.flashing("success" -> "Relación aceptada y guardad")
      else
        result.flashing("error" -> "ERROR EN GUARDAR LA RELACION")
    }
  }
  def decline(discordUserID: String,matchID: UUID): Action[AnyContent] = silhouette.SecuredAction(WithAdmin()).async { implicit request =>
    for {
      accepted <- smurfService.declineSmurf(discordUserID, matchID)

    } yield {

      val result = Redirect(routes.SmurfController.view())

      if(accepted)
        result.flashing("success" -> "Relación denegada, todos los replays asociados a ese usuario-smurf se eliminaron")
      else
        result.flashing("error" -> "ERROR en eliminar la relacion y smurfs")
    }
  }
  def showListSmurfsDefined(): Action[AnyContent] = Action.async{ implicit request =>
    smurfService.loadValidSmurfs().map{ usuarios =>
      val json = Json.obj("users" ->
        Json.arr(usuarios.map(u =>
          //FixMe find and use correct discordname
          Json.obj("discordname" -> "UNKNOWN",
                          "smurfs" -> Json.arr(u.smurfs.map(_.name)),
                          "discordID" -> u.discordID.id))))
      Ok(json)
    }

  }

}
