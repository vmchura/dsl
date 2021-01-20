package modules.winnersgeneration

import models.{DiscordID, TournamentSerieID}
import play.api.data.Form
import play.api.data.Forms._

object WinnersForm {
  val winnerForm: Form[WinnersInformation] = Form(
    mapping(
      "tournamentID" -> longNumber,
      "tournamentSeriesID" -> uuid,
      "season" -> number,
      "player1" -> nonEmptyText,
      "player2" -> nonEmptyText,
      "player3" -> nonEmptyText
    ) { (tournamentID, tournamentSeriesID, season, player1, player2, player3) =>
      WinnersInformation(
        tournamentID,
        TournamentSerieID(tournamentSeriesID),
        List(
          (1, DiscordID(player1)),
          (2, DiscordID(player2)),
          (3, DiscordID(player3))
        ),
        season
      )
    } { wi =>
      {
        if (wi.players.map(_._1).sorted == List(1, 2, 3)) {
          val pid = wi.players.map { case (x, y) => x -> y.id }.toMap
          Some(
            (
              wi.tournamentID,
              wi.tournamentSeries.id,
              wi.season,
              pid(1),
              pid(2),
              pid(3)
            )
          )
        } else {
          None
        }
      }
    }
  )
}
