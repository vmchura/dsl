package models

import java.util.UUID

import play.api.libs.json._
import shared.utils.ComparableByLabel


case class ParticipantPK(challongeID: Long,chaNameID: Long)
object ParticipantPK{
  implicit val jsonFormat: OFormat[ParticipantPK] = Json.format[ParticipantPK]
}

case class Participant(participantPK: ParticipantPK, chaname: String, discordUserID: Option[String], userID: Option[UUID]) extends ComparableByLabel {
  override def stringLabel: String = chaname
}
object Participant {
  implicit val jsonFormat: OFormat[Participant] = Json.format[Participant]
}

