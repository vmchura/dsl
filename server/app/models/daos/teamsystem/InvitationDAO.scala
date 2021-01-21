package models.daos.teamsystem

import models.DiscordID
import models.teamsystem.{Invitation, InvitationID}

import scala.concurrent.Future

trait InvitationDAO {
  def invitationsToUser(userID: DiscordID): Future[Seq[Invitation]]
  def invitationsFromUser(userID: DiscordID): Future[Seq[Invitation]]
  def addInvitation(invitation: Invitation): Future[InvitationID]
  def removeInvitation(invitationID: InvitationID): Future[Boolean]
}
