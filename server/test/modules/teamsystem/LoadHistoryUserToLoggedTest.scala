package modules.teamsystem

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import models.daos.UserHistoryDAO
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

class LoadHistoryUserToLoggedTest
    extends ScalaTestWithActorTestKit
    with AnyWordSpecLike
    with GuiceOneAppPerSuite {

  "Insert data" must {
    "Into logged" in {
      val userDAO = app.injector.instanceOf(classOf[UserHistoryDAO])
      val ids = userDAO.all().futureValue.map(_.discordID).distinct.take(4)
      val workerBehavior = app.injector.instanceOf(classOf[DiscordPlayerWorker])
      val probe = testKit
        .createTestProbe[DiscordPlayerWorker.DiscordPlayerWorkerResponse]("")
      val supervisor = testKit.spawn(DiscordPlayerSupervisor(workerBehavior))
      ids.foreach { id =>
        println(id)
        supervisor ! DiscordPlayerSupervisor.Register(id, Some(probe.ref))
        probe.expectMessage(10 seconds, DiscordPlayerWorker.Registered())
      }
    }
  }

}
