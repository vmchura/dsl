package modules.teamsystem

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import models.daos.UserHistoryDAO
import org.scalatest.Ignore
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

@Ignore
class LoadHistoryUserToLoggedTest
    extends ScalaTestWithActorTestKit
    with AnyWordSpecLike
    with GuiceOneAppPerSuite {

  "Insert data" must {
    "Into logged" in {
      val userDAO = app.injector.instanceOf(classOf[UserHistoryDAO])
      val ids = userDAO.all().futureValue.map(_.discordID).distinct.drop(40)
      val workerBehavior = app.injector.instanceOf(classOf[DiscordPlayerWorker])
      val probe = testKit
        .createTestProbe[DiscordPlayerWorker.DiscordPlayerWorkerResponse]("")
      val supervisor = testKit.spawn(DiscordPlayerSupervisor(workerBehavior))
      ids.zipWithIndex.foreach {
        case (id, indx) =>
          Thread.sleep((3 seconds).toMillis)
          supervisor ! DiscordPlayerSupervisor.Register(id, Some(probe.ref))
          val messageResponse = probe.receiveMessage(10 seconds)
          println(s"$indx] $messageResponse")
      }
    }
  }

}
