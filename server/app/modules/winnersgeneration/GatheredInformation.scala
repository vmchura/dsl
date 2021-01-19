package modules.winnersgeneration

import models.{Tournament, TournamentSeries, UserGuild}

case class GatheredInformation(
    tournaments: Seq[Tournament],
    tournamentSeries: Seq[TournamentSeries],
    users: Seq[UserGuild]
)
