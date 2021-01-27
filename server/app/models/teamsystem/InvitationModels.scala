package models.teamsystem

import models.daos.DiscordPlayerLoggedDAO
import models.daos.teamsystem.{InvitationDAO, TeamDAO}
import play.api.libs.json.{Json, OFormat}
import shared.models.{DiscordID, DiscordPlayerLogged}

import java.util.UUID
import scala.concurrent.Future

case class InvitationID(id: UUID) extends AnyVal
object InvitationID {
  implicit val jsonFormat: OFormat[InvitationID] =
    Json.format[InvitationID]
}

case class Invitation(
    invitationID: InvitationID,
    from: DiscordID,
    to: DiscordID,
    teamID: TeamID,
    status: MemberStatus
)
object Invitation {
  import models.ModelsJsonImplicits._
  implicit val jsonFormat: OFormat[Invitation] =
    Json.format[Invitation]
}

case class InvitationMeta(to: DiscordID, status: MemberStatus)

case class InvitationWithUsers(
    invitationID: InvitationID,
    from: DiscordPlayerLogged,
    to: DiscordPlayerLogged,
    teamName: String,
    status: MemberStatus
)
object InvitationWithUsers {
  import scala.concurrent.ExecutionContext.Implicits.global
  def apply(invitation: Invitation)(implicit
      discordPlayerLoggedDAO: DiscordPlayerLoggedDAO,
      teamDAO: TeamDAO
  ): Future[Option[InvitationWithUsers]] = {
    val loader: DiscordID => Future[Option[DiscordPlayerLogged]] =
      discordPlayerLoggedDAO.load
    val teamLoader: TeamID => Future[Option[Team]] = teamDAO.loadTeam

    for {
      fromFut <- loader(invitation.from)
      toFut <- loader(invitation.to)
      teamFut <- teamLoader(invitation.teamID)
    } yield {
      for {
        from <- fromFut
        to <- toFut
        team <- teamFut
      } yield {
        InvitationWithUsers(
          invitation.invitationID,
          from,
          to,
          team.teamName,
          invitation.status
        )
      }
    }
  }
}
