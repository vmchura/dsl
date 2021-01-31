package models.teamsystem

import models.daos.DiscordPlayerLoggedDAO
import models.daos.teamsystem.TeamDAO

import java.util.UUID
import play.api.libs.json.{Json, OFormat}
import shared.models.{DiscordID, DiscordPlayerLogged}

import scala.concurrent.Future
case class RequestJoinID(id: UUID) extends AnyVal
object RequestJoinID {
  implicit val jsonFormat: OFormat[RequestJoinID] =
    Json.format[RequestJoinID]
}
case class RequestJoin(
    requestID: RequestJoinID,
    from: DiscordID,
    teamID: TeamID
)
object RequestJoin {
  import models.ModelsJsonImplicits._
  implicit val jsonFormat: OFormat[RequestJoin] = Json.format[RequestJoin]
}
case class RequestJoinMeta(from: DiscordID, teamID: TeamID)

case class RequestJoinWithUsers(
    requestID: RequestJoinID,
    from: DiscordPlayerLogged,
    teamID: TeamID,
    teamName: String
)

object RequestJoinWithUsers {
  import scala.concurrent.ExecutionContext.Implicits.global
  def apply(request: RequestJoin)(implicit
      discordPlayerLoggedDAO: DiscordPlayerLoggedDAO,
      teamDAO: TeamDAO
  ): Future[Option[RequestJoinWithUsers]] = {
    val loader: DiscordID => Future[Option[DiscordPlayerLogged]] =
      discordPlayerLoggedDAO.load
    val teamLoader: TeamID => Future[Option[Team]] = teamDAO.loadTeam

    for {
      fromFut <- loader(request.from)
      teamFut <- teamLoader(request.teamID)
    } yield {
      for {
        from <- fromFut
        team <- teamFut
      } yield {
        RequestJoinWithUsers(
          request.requestID,
          from,
          team.teamID,
          team.teamName
        )
      }
    }
  }
}
