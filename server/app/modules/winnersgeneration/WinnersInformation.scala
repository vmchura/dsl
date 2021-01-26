package modules.winnersgeneration

import models.TournamentSerieID
import shared.models.DiscordID

case class WinnersInformation(
    tournamentID: Long,
    tournamentSeries: TournamentSerieID,
    players: List[(Int, DiscordID)],
    season: Int
)
