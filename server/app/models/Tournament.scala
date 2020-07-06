package models

import java.util.UUID

import play.api.libs.json._

case class Tournament(tournamentID: UUID, challongID: String, discordServerID: String, tournamentName: String,active: Boolean)

object Tournament {
  implicit val jsonFormat: OFormat[Tournament] = Json.format[Tournament]
}