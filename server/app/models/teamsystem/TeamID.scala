package models.teamsystem

import models.DiscordID

import java.util.UUID

case class TeamID(id: UUID) extends AnyVal
sealed trait MemberStatus
case object Official extends MemberStatus
case object Suplente extends MemberStatus
case class Member(userID: DiscordID, memberStatus: MemberStatus)

case class Team(teamID: TeamID, principal: DiscordID, members: Seq[Member]) {

  def canBeAdded(userID: DiscordID): Boolean = !isMember(userID)
  def canBeAddedAsSuplente(userID: DiscordID): Boolean =
    canBeAdded(userID) && !members.exists(_.memberStatus == Suplente)
  def isOfficial(userID: DiscordID): Boolean =
    members.exists(m => m.userID == userID && m.memberStatus == Official)
  def isMember(userID: DiscordID): Boolean = members.exists(_.userID == userID)
}
