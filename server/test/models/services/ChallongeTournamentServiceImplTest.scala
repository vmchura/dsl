package models.services

import scala.concurrent.ExecutionContext.Implicits.global
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

class ChallongeTournamentServiceImplTest extends PlaySpec with GuiceOneAppPerSuite {
  val service: ChallongeTournamentService = app.injector.instanceOf(classOf[ChallongeTournamentService])

  "A Discord User Service" should {
    "get correct users" in {
      service.findChallongeTournament("-")("DeathfateStarLeague").map {
        tournament =>
          assertResult(Some(8415514))(tournament.map(_.tournament.challongID))
      }

    }
  }
}
