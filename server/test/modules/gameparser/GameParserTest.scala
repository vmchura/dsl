package modules.gameparser
import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import models.StarCraftModels._
import modules.gameparser.GameParser.Team
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

import java.io.File

class GameParserTest
    extends ScalaTestWithActorTestKit
    with AnyWordSpecLike
    with GuiceOneAppPerSuite {
  private val gameParseFactory =
    app.injector.instanceOf(classOf[GameParserFactory])

  "GameParser" must {
    "parse file correctly" in {
      val file = new File(
        "/home/vmchura/Games/screp/cmd/screp/R_G19P----_ChesterP_83402431.rep"
      )
      val parser = testKit.spawn(gameParseFactory.create(), "parser")
      val probe = testKit.createTestProbe[GameParser.GameInfo]()
      parser ! GameParser.ReplayToParse(file, probe.ref)
      probe.expectMessage(
        GameParser.ReplayParsed(
          TopVsBottom,
          List(
            Team(1, List(SCPlayer("G19", Protoss))),
            Team(2, List(SCPlayer(".Chester", Protoss)))
          ),
          1
        )
      )
    }
  }
}
