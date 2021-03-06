package models.teamsystem

import models.daos.DiscordPlayerLoggedDAO
import shared.models.{DiscordID, DiscordPlayerLogged, ReplayTeamID}
import play.api.libs.json._
import shared.models.StarCraftModels.StringDate

import java.text.SimpleDateFormat
import scala.concurrent.ExecutionContext.Implicits.global
import java.util.Date
import scala.concurrent.Future
case class Points(
    teamID: TeamID,
    replayTeamID: ReplayTeamID,
    points: Int,
    userDiscordID: DiscordID,
    date: StringDate,
    reason: String,
    enabled: Boolean
)
object Points {
  import models.ModelsJsonImplicits._
  implicit val pointsJsonFormat: OFormat[Points] =
    Json.format[Points]
}
case class PointsWithUser(
    user: DiscordPlayerLogged,
    date: String,
    reason: String,
    puntos: Int,
    enabled: Boolean
)
object PointsWithUser {
  def apply(points: Points)(implicit
      discordPlayerLoggedDAO: DiscordPlayerLoggedDAO
  ): Future[Option[PointsWithUser]] = {
    discordPlayerLoggedDAO.load(points.userDiscordID).map {
      _.map(user =>
        new PointsWithUser(
          user,
          new SimpleDateFormat("dd-MMM-yyyy")
            .format(points.date.toDate),
          points.reason,
          points.points,
          points.enabled
        )
      )
    }
  }
}
