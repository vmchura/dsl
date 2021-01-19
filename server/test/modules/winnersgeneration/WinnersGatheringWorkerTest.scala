package modules.winnersgeneration

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import modules.winnersgeneration.WinnersGathering.{
  GatheringComplete,
  GatheringSucess
}
import modules.winnersgeneration.WinnersGatheringWorker.GatheredInformationBuilder
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
class WinnersGatheringWorkerTest
    extends ScalaTestWithActorTestKit
    with AnyWordSpecLike
    with GuiceOneAppPerSuite {

  "WinnersGatheringWorker" must {
    "Build correctly" in {
      val replyTo =
        testKit.createTestProbe[GatheringComplete](s"probe-replyTo")
      val origin =
        testKit.createTestProbe[GatheringSucess](s"probe-origin")

      val worker = testKit.spawn(
        WinnersGatheringWorker(
          GatheredInformationBuilder(),
          replyTo.ref,
          origin.ref
        )
      )

      worker ! WinnersGatheringWorker.TournamentSeriesGathered(Nil)
      worker ! WinnersGatheringWorker.TournamentsGathered(Nil)
      worker ! WinnersGatheringWorker.UsersGathered(Nil)
      replyTo.expectMessage(
        GatheringComplete(origin.ref, GatheredInformation(Nil, Nil, Nil))
      )

    }
  }
}
