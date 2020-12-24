package database

import models.services.TournamentService
import org.scalatestplus.play.PlaySpec

class LocalTemporalDB extends PlaySpec with TemporalDB {
  "Tournament" should {
    "Not be inserted" in {
      val tournamentService: TournamentService =
        app.injector.instanceOf(classOf[TournamentService])
      assert(tournamentService.findAllTournaments().futureValue.toList.isEmpty)
    }
    "Be inserted" in {
      val tournamentService: TournamentService =
        app.injector.instanceOf(classOf[TournamentService])
      initTournament()
      val tournaments =
        tournamentService.findAllTournaments().futureValue.toList
      assert(tournaments.nonEmpty)
    }
  }
}
