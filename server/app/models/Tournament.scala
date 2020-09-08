package models


import models.services.ChallongeTournamentService
import play.api.libs.json._

case class Tournament(challongeID: Long, urlID: String, discordServerID: String, tournamentName: String,active: Boolean, channelDiscordReplay: Option[String] = None){
  val challongeURL: String = ChallongeTournamentService.buildUri(urlID)
}

object Tournament {
  implicit val jsonFormat: OFormat[Tournament] = Json.format[Tournament]
}
