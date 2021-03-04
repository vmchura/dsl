package models.teamsystem

import models.daos.DiscordPlayerLoggedDAO
import shared.models.{DiscordPlayerLogged, ReplayTeamID}

import scala.concurrent.Future

case class PendingSmurfWithUser(
    discordUser: DiscordPlayerLogged,
    smurf: models.Smurf,
    replayTeamID: ReplayTeamID
)
object PendingSmurfWithUser {
  import scala.concurrent.ExecutionContext.Implicits.global
  def apply(pendingSmurf: PendingSmurf)(implicit
      discordPlayerLoggedDAO: DiscordPlayerLoggedDAO
  ): Future[Option[PendingSmurfWithUser]] = {
    discordPlayerLoggedDAO.load(pendingSmurf.discordID).map {
      _.map(logged =>
        new PendingSmurfWithUser(
          logged,
          pendingSmurf.smurf,
          pendingSmurf.replayTeamID
        )
      )

    }
  }
}
