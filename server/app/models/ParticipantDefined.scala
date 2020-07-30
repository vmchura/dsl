package models

import java.util.UUID

import shared.utils.ComparableByLabel

case class ParticipantDefined(participantPK: ParticipantPK, chaname: String, discordUserID: String, userID: Option[UUID]) extends ComparableByLabel {
  override def stringLabel: String = chaname
}
