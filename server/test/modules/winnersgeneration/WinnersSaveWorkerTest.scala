package modules.winnersgeneration

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import modules.winnersgeneration.WinnersSaving.{
  SaveWinnersCompleted,
  WinnersSavingResponse
}
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

class WinnersSaveWorkerTest
    extends ScalaTestWithActorTestKit
    with AnyWordSpecLike
    with GuiceOneAppPerSuite {

  "WinnersSavingWorker" must {
    "Build correctly" in {
      val replyTo =
        testKit.createTestProbe[SaveWinnersCompleted](s"probe-replyTo")
      val origin =
        testKit.createTestProbe[WinnersSavingResponse](s"probe-origin")

      val worker = testKit.spawn(
        WinnersSaveWorker(
          replyTo.ref,
          origin.ref
        )
      )

      worker ! WinnersSaveWorker.TournamentSeasonSaved()
      replyTo.expectMessage(
        SaveWinnersCompleted(origin.ref)
      )

    }
  }
}
