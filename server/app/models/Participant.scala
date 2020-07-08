package models

import java.util.UUID
import play.api.libs.json._


case class ParticipantPK(tournamentID: UUID,chaNameID: Long)
object ParticipantPK{
  implicit val jsonFormat: OFormat[ParticipantPK] = Json.format[ParticipantPK]
}

case class Participant(participantPK: ParticipantPK, discordUserID: Option[String], userID: Option[UUID])
object Participant {
  implicit val jsonFormat: OFormat[Participant] = Json.format[Participant]
}

