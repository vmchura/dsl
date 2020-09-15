package controllers

import javax.inject.Inject
import models.services.SideBarMenuService
import org.joda.time.DateTime
import play.api.i18n.I18nSupport
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent}

import scala.concurrent.{ExecutionContext, Future}

class UserStatsAPI @Inject()(scc: SilhouetteControllerComponents,
                             sideBarMenuService: SideBarMenuService,
                              viewStatsUser: views.html.userstats
                            ) (
                              implicit
                              assets: AssetsFinder,
                              ex: ExecutionContext
                            )extends   AbstractAuthController(scc) with I18nSupport {
  def buildRelation(discordUserID: String): Action[AnyContent] = Action.async{
    Future.successful{
      Ok(Json.obj(
        "userName" -> "Krakatoa",
        "info" -> Json.obj(
          "mainRace" -> "Terran",
          "results" -> Json.arr(
            Json.obj("tournamentName" -> "ASL",
              "season" -> "1",
              "challongeURL" -> "https://challonge.com/ozrcvcvb",
              "position" -> 1),
            Json.obj("tournamentName" -> "ASL",
              "season" -> "3",
              "challongeURL" -> "https://challonge.com/ozrcvcvb",
              "position" -> 2),
            Json.obj("tournamentName" -> "ASL",
              "season" -> "2",
              "challongeURL" -> "https://challonge.com/ozrcvcvb",
              "position" -> 5)
          )
        ),
        "matches" -> Json.obj(
          "vsP" -> 45,
          "vsT" -> 45,
          "vsZ" -> 80,
          "importantMatches" -> Json.arr(
            Json.obj("date" -> "2010-14-12",
            "event" -> Json.obj("tournamentName" -> "ASL","season" -> 1),
              "youtubecast" -> "https://www.youtube.com/watch?v=LFoQqAI2LSo&t=3042s",
              "player1" -> Json.obj("userName" -> "krakatoa", "discordUserID" -> "123Discord"),
              "player2" -> Json.obj("userName" -> "Bisu", "discordUserID" -> "bisuDiscord"),
            ),
            Json.obj("date" -> "2010-14-13",
              "event" -> Json.obj("tournamentName" -> "ASL","season" -> 2),
              "youtubecast" -> "https://www.youtube.com/watch?v=LFoQqAI2LSo&t=3042s",
              "player1" -> Json.obj("userName" -> "Bisu", "discordUserID" -> "BisuDiscord"),
              "player2" -> Json.obj("userName" -> "Krakatoa", "discordUserID" -> "krakenDiscord"),
            )
          )
        )
      ))
    }
  }
  def viewMyStats() = SecuredAction.async{ implicit request =>
    sideBarMenuService.buildLoggedSideBar().map { implicit menues =>
      Ok(viewStatsUser(request.identity,  socialProviderRegistry))
    }
  }
}
