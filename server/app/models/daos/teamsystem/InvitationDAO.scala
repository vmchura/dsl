package models.daos.teamsystem

import models.teamsystem.{Invitation, InvitationID}
import shared.models.DiscordID

import scala.concurrent.Future

trait InvitationDAO {
  def loadInvitation(invitationID: InvitationID): Future[Option[Invitation]]
  def invitationsToUser(userID: DiscordID): Future[Seq[Invitation]]
  def invitationsFromUser(userID: DiscordID): Future[Seq[Invitation]]
  def addInvitation(invitation: Invitation): Future[InvitationID]
  def removeInvitation(invitationID: InvitationID): Future[Boolean]
}
