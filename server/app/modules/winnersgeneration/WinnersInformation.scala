package modules.winnersgeneration

import models.{DiscordID, TournamentSerieID}

case class WinnersInformation(
    tournamentID: Long,
    tournamentSeries: TournamentSerieID,
    players: List[(Int, DiscordID)],
    season: Int
)
