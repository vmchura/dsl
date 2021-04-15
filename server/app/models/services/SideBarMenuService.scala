package models.services

import com.mohiva.play.silhouette.api.actions.{SecuredRequest, UserAwareRequest}
import controllers.{WithAdmin, routes}
import models.daos.DiscordPlayerLoggedDAO

import javax.inject.Inject
import models.{ExtraAction, MenuActionDefined, MenuGroup, Tournament, User}
import shared.models.{DiscordDiscriminator, DiscordID}
import utils.auth.DefaultEnv

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SideBarMenuService @Inject() (
    tournamentService: TournamentService,
    discordPlayerLoggedDAO: DiscordPlayerLoggedDAO
) {

  private def buildSideBar(
      user: Option[User]
  ): Future[(Seq[MenuGroup], Option[DiscordDiscriminator])] = {
    for {
      torneos <- tournamentService.findAllActiveTournaments()
      registered <- user.fold(Future.successful(Seq.empty[Tournament]))(u =>
        tournamentService.findAllActiveTournamentsByPlayer(
          u.loginInfo.providerKey
        )
      )
      logged <- user.fold(
        Future.successful(Option.empty[DiscordDiscriminator])
      )(u =>
        discordPlayerLoggedDAO
          .load(DiscordID(u.loginInfo.providerKey))
          .map(_.map(_.discriminator))
      )
    } yield {

      val misTorneos = user.map(_ =>
        MenuGroup(
          "Mis Partidas",
          registered.map(torneo =>
            MenuActionDefined(
              torneo.tournamentName,
              routes.TournamentController
                .showMatchesToUploadReplay(torneo.challongeID)
                .url
            )
          )
        )
      )
      val todos = MenuGroup(
        "Torneos",
        torneos
          .map(torneo =>
            MenuActionDefined(
              torneo.tournamentName,
              routes.TournamentController.showMatches(torneo.challongeID).url
            )
          )
          .toList ::: ExtraAction(
          "Todos los torneos",
          routes.StaticsController.view().url
        ) :: Nil
      )
      val equipos = MenuGroup(
        "Equipos",
        ExtraAction(
          "Listar equipos",
          controllers.teamsystem.routes.TeamManagerController.showAllTeams().url
        ) ::
          MenuActionDefined(
            "Mi equipo",
            controllers.teamsystem.routes.TeamManagerController
              .showMyTeams()
              .url
          ) ::
          MenuActionDefined(
            "Subir replay - equipo",
            controllers.teamsystem.routes.TeamReplayController
              .view()
              .url
          )
          :: Nil
      )
      val admin = user.flatMap(u =>
        if (WithAdmin.isModeradorID(u.loginInfo.providerKey))
          Some(
            MenuGroup(
              "Admin",
              List(
                MenuActionDefined("Smurfs", routes.SmurfController.view().url)
              )
            )
          )
        else None
      )

      (List(misTorneos, Some(todos), Some(equipos), admin).flatten, logged)
    }
  }

  def buildLoggedSideBar()(implicit
      request: SecuredRequest[DefaultEnv, _]
  ): Future[(Seq[MenuGroup], Option[DiscordDiscriminator])] =
    buildSideBar(Some(request.identity))
  def buildUserAwareSideBar()(implicit
      request: UserAwareRequest[DefaultEnv, _]
  ): Future[(Seq[MenuGroup], Option[DiscordDiscriminator])] =
    buildSideBar(request.identity)
  def buildGuestSideBar()
      : Future[(Seq[MenuGroup], Option[DiscordDiscriminator])] =
    buildSideBar(None)

}
