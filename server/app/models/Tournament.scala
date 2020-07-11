package models


import play.api.libs.json._

case class Tournament(challongeID: Long, discordServerID: String, tournamentName: String,active: Boolean)

object Tournament {
  implicit val jsonFormat: OFormat[Tournament] = Json.format[Tournament]
}