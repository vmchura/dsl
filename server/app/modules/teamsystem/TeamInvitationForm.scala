package modules.teamsystem

import models.teamsystem.{InvitationMeta, MemberStatus}
import play.api.data.Form
import play.api.data.Forms._
import shared.models.DiscordID
object TeamInvitationForm {
  val teamInvitation: Form[InvitationMeta] = Form(
    mapping(
      "to" -> text,
      "status" -> text
    )((to, status) => InvitationMeta(DiscordID(to), MemberStatus(status)))(im =>
      Some((im.to.id, im.status.name))
    )
  )

}
