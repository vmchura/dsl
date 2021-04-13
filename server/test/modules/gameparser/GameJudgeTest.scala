package modules.gameparser

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.actor.typed.ActorRef
import models.services.ParseReplayFileService
import modules.gameparser.GameReplayManager.{ManageGameReplay, ManagerCommand}
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

import java.io.File
import shared.models.StarCraftModels._
class GameJudgeTest
    extends ScalaTestWithActorTestKit
    with AnyWordSpecLike
    with GuiceOneAppPerSuite {
  private val replayParseService =
    app.injector.instanceOf(classOf[ParseReplayFileService])
  private val gameReplayJudger: ActorRef[ManagerCommand] =
    testKit.spawn(GameReplayManager.create(replayParseService))

  "GameJudger" must {
    "judge games correctly" in {
      val input = Seq(
        (
          "/home/vmchura/Games/screp/cmd/screp/R_G19P----_ChesterP_83402431.rep",
          OneVsOne(
            winner = SCPlayer("G19", Protoss),
            loser = SCPlayer(".Chester", Protoss),
            "",
            StringDate("")
          )
        ),
        (
          "/home/vmchura/Games/screp/cmd/screp/DSL5_AdarizvsCanzitog2.rep",
          OneVsOne(
            winner = SCPlayer("Aldarizzz", Zerg),
            loser = SCPlayer("cafirdo", Zerg),
            "",
            StringDate("")
          )
        ),
        (
          "/home/vmchura/Games/screp/cmd/screp/R_IzaaP---_ReyAzucarZ_14240114.rep",
          OneVsOne(
            winner = SCPlayer("ReyAzucar", Zerg),
            loser = SCPlayer("I.zA.a", Protoss),
            "",
            StringDate("")
          )
        )
      )
      def assertParse(pathFile: String, result: SCGameMode): Unit = {
        val file = new File(pathFile)
        val probe =
          testKit.createTestProbe[SCGameMode](s"probe-${pathFile.length}")

        gameReplayJudger ! ManageGameReplay(file, probe.ref)
        probe.expectMessage(result)
      }

      input.foreach {
        case (path, result) => assertParse(path, result)
      }
    }
  }
}
