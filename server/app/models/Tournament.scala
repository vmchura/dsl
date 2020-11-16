package models

import java.util.UUID

import models.services.ChallongeTournamentService
import play.api.libs.json._

case class Tournament(
    challongeID: Long,
    urlID: String,
    discordServerID: String,
    tournamentName: String,
    active: Boolean,
    channelDiscordReplay: Option[String] = None
) {
  val challongeURL: String = ChallongeTournamentService.buildUri(urlID)
}

object Tournament {
  implicit val jsonFormat: OFormat[Tournament] = Json.format[Tournament]
}

case class TournamentSeason(
    challongeID: Long,
    season: Int,
    winners: List[(Int, String)]
)
object TournamentSeason {
  implicit val jsonFormat: OFormat[TournamentSeason] =
    Json.format[TournamentSeason]
}
case class TournamentSeasonFilled(
    challongeID: Long,
    season: Int,
    winners: List[(Int, DiscordUser)]
)

case class TournamentSeriesFilled(
    id: TournamentSerieID,
    name: String,
    seasons: Seq[TournamentSeasonFilled]
)
case class TournamentSerieID(id: UUID) extends AnyVal
object TournamentSerieID {
  implicit val jsonFormat: OFormat[TournamentSerieID] =
    Json.format[TournamentSerieID]
}
case class TournamentSeries(
    id: TournamentSerieID,
    name: String,
    image: Option[String],
    color: String,
    seasons: Seq[TournamentSeason]
)
object TournamentSeries {
  implicit val jsonFormat: OFormat[TournamentSeries] =
    Json.format[TournamentSeries]
}
