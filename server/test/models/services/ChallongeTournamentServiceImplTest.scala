package models.services

import scala.concurrent.ExecutionContext.Implicits.global
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

class ChallongeTournamentServiceImplTest extends PlaySpec with GuiceOneAppPerSuite {
  val service: ChallongeTournamentService = app.injector.instanceOf(classOf[ChallongeTournamentService])

  "A Challonge Tournament Service" should {
    "get correct tournament" in {
      val queryExecution = service.findChallongeTournament("-")("DeathfateStarLeague").map {
        tournament => {
          assertResult(Some(8415514))(tournament.map(_.tournament.challongeID))
        }

      }
      Await.result(queryExecution, 30 seconds)
      queryExecution
    }

    "get correct tournament past group phase" in {
      val queryExecution = service.findChallongeTournament("-")("DeathfateChallengerStarLeague1").map {
        tournament =>{
          assertResult(Some(8588305))(tournament.map(_.tournament.challongeID))
        }

      }
      Await.result(queryExecution,30 seconds)
      queryExecution
    }



  }
}
