package models.teamsystem

import play.api.libs.json.{Json, OFormat}
import shared.models.DiscordID

import java.util.UUID

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
