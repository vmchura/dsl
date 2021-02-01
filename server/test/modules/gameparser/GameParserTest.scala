package modules.gameparser
import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import models.services.ParseReplayFileService
import modules.gameparser.GameParser.Team
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import shared.models.StarCraftModels._

import java.io.File
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

class GameParserTest
    extends ScalaTestWithActorTestKit
    with AnyWordSpecLike
    with GuiceOneAppPerSuite {
  private val replayParseService =
    app.injector.instanceOf(classOf[ParseReplayFileService])

  "GameParser" must {
    "parse file correctly" in {
      val file = new File(
        "/home/vmchura/Games/screp/cmd/screp/R_G19P----_ChesterP_83402431.rep"
      )
      val parser = testKit.spawn(GameParser(replayParseService), "parser")
      val probe = testKit.createTestProbe[GameParser.GameInfo]()
      parser ! GameParser.ReplayToParse(file, probe.ref)

      val messageExpected = GameParser.ReplayParsed(
        Some("Fighting Spirit"),
        Some("2020-12-04T22:05:16Z"),
        TopVsBottom,
        List(
          Team(1, List(SCPlayer("G19", Protoss))),
          Team(2, List(SCPlayer(".Chester", Protoss)))
        ),
        1
      )
      val messageGot =
        probe.receiveMessage(10 seconds).asInstanceOf[GameParser.ReplayParsed]
      if (
        messageGot.mapName.zip(messageExpected.mapName).fold(false) {
          case (got, exp) => got.contains(exp)
        }
      ) {
        val messageGotChangedMapName =
          messageGot.copy(mapName = messageExpected.mapName)
        assertResult(messageExpected)(messageGotChangedMapName)
      } else {
        fail("Map names are not present")
      }

    }
    "Parse KaoZerg vs Uzumaki 1 " in {
      val file = new File(
        "/home/vmchura/Documents/uzumaki_vs_KaoZerg_1.rep"
      )
      val parser = testKit.spawn(GameParser(replayParseService), "parser")
      val probe = testKit.createTestProbe[GameParser.GameInfo]()
      parser ! GameParser.ReplayToParse(file, probe.ref)

      val messageExpected = GameParser.ReplayParsed(
        Some("Fighting Spirit"),
        Some("2021-01-31T23:13:29Z"),
        TopVsBottom,
        List(
          Team(1, List(SCPlayer("Uzumaki809", Protoss))),
          Team(2, List(SCPlayer("KaoZerG", Zerg)))
        ),
        2
      )
      val messageGot =
        probe.receiveMessage(10 seconds).asInstanceOf[GameParser.ReplayParsed]
      if (
        messageGot.mapName.zip(messageExpected.mapName).fold(false) {
          case (got, exp) => got.contains(exp)
        }
      ) {
        val messageGotChangedMapName =
          messageGot.copy(mapName = messageExpected.mapName)
        assertResult(messageExpected)(messageGotChangedMapName)
      } else {
        fail("Map names are not present")
      }
    }
    "Parse KaoZerg vs Uzumaki 2" in {
      val file = new File(
        "/home/vmchura/Documents/uzumaki_vs_KaoZerg_2.rep"
      )
      val parser = testKit.spawn(GameParser(replayParseService), "parser")
      val probe = testKit.createTestProbe[GameParser.GameInfo]()
      parser ! GameParser.ReplayToParse(file, probe.ref)

      val messageExpected = GameParser.ReplayParsed(
        Some("Polyp"),
        Some("2021-01-31T23:43:20Z"),
        TopVsBottom,
        List(
          Team(1, List(SCPlayer("Uzumaki809", Protoss))),
          Team(2, List(SCPlayer("KaoZerG", Zerg)))
        ),
        1
      )
      val messageGot =
        probe.receiveMessage(10 seconds).asInstanceOf[GameParser.ReplayParsed]
      if (
        messageGot.mapName.zip(messageExpected.mapName).fold(false) {
          case (got, exp) => got.contains(exp)
        }
      ) {
        val messageGotChangedMapName =
          messageGot.copy(mapName = messageExpected.mapName)
        assertResult(messageExpected)(messageGotChangedMapName)
      } else {
        fail("Map names are not present")
      }
    }
  }
}
