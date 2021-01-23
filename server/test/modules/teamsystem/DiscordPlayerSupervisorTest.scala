package modules.teamsystem

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import database.EmptyDBBeforeEach
import models.DiscordID
import models.daos.DiscordPlayerLoggedDAO
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

class DiscordPlayerSupervisorTest
    extends ScalaTestWithActorTestKit
    with AnyWordSpecLike
    with GuiceOneAppPerSuite
    with EmptyDBBeforeEach {
  "DiscordPlayerSupervisor" must {
    "Register my discord info" in {
      val discordPlayerWorker =
        app.injector.instanceOf(classOf[DiscordPlayerWorker])
      val discordPlayerSupervisor = DiscordPlayerSupervisor(discordPlayerWorker)
      val actor = testKit.spawn(discordPlayerSupervisor, "discord-supervisor")
      val probe = testKit
        .createTestProbe[DiscordPlayerWorker.DiscordPlayerWorkerResponse](
          "probe-app"
        )
      actor ! DiscordPlayerSupervisor.Register(
        DiscordID("698648718999814165"),
        Some(probe.ref)
      )
      probe.expectMessage(10 seconds, DiscordPlayerWorker.Registered())
      val discordPlayerDAO =
        app.injector.instanceOf(classOf[DiscordPlayerLoggedDAO])
      whenReady(discordPlayerDAO.load(DiscordID("698648718999814165"))) {
        discordInfo =>
          assertResult(Some("5436"))(discordInfo.map(_.discriminator.value))

      }
    }
    "FireWall bot id" in {
      val discordPlayerWorker =
        app.injector.instanceOf(classOf[DiscordPlayerWorker])
      val discordPlayerSupervisor = DiscordPlayerSupervisor(discordPlayerWorker)
      val actor =
        testKit.spawn(discordPlayerSupervisor, "discord-supervisor-bot")
      val probe = testKit
        .createTestProbe[DiscordPlayerWorker.DiscordPlayerWorkerResponse](
          "probe-app-bot"
        )
      actor ! DiscordPlayerSupervisor.Register(
        DiscordID("713047985193353218"),
        Some(probe.ref)
      )
      probe.expectMessageType[DiscordPlayerWorker.DiscordPlayerWorkerError](
        10 seconds
      )
      val discordPlayerDAO =
        app.injector.instanceOf(classOf[DiscordPlayerLoggedDAO])
      whenReady(discordPlayerDAO.load(DiscordID("713047985193353218"))) {
        discordInfo =>
          assertResult(None)(discordInfo.map(_.discriminator.value))

      }
    }
  }
}
