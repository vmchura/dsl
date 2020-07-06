package models

import java.util.UUID
import play.api.libs.json._


case class ParticipantPK(tournamentID: UUID,chanameID: String)
object ParticipantPK{
  implicit val jsonFormat: OFormat[ParticipantPK] = Json.format[ParticipantPK]
}

case class Participant(participantPK: ParticipantPK, discordUserID: Option[String], userID: Option[UUID])
object Participant {
  import ParticipantPK.jsonFormat
  implicit val jsonFormat: OFormat[Participant] = Json.format[Participant]

}

