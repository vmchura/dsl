package modules.gameparser

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.actor.typed.scaladsl.Behaviors
import models.{InvalidSCGameMode, OneVsOne, Protoss, SCGameMode, SCPlayer, Zerg}
import modules.gameparser.GameJudge.JudgeGame
import modules.gameparser.GameParser._
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

import java.io.File

class GameJudgeTest
    extends ScalaTestWithActorTestKit
    with AnyWordSpecLike
    with GuiceOneAppPerSuite {

  private val gameParseFactory =
    app.injector.instanceOf(classOf[GameParserFactory])

  "GameJudger" must {
    "judge games correctly" in {
      val input = Seq(
        (
          "/home/vmchura/Games/screp/cmd/screp/R_G19P----_ChesterP_83402431.rep",
          OneVsOne(
            winner = SCPlayer("G19", Protoss),
            loser = SCPlayer(".Chester", Protoss)
          )
        ),
        (
          "/home/vmchura/Games/screp/cmd/screp/DSL5_AdarizvsCanzitog2.rep",
          OneVsOne(
            winner = SCPlayer("Aldarizzz", Zerg),
            loser = SCPlayer("cafirdo", Zerg)
          )
        ),
        (
          "/home/vmchura/Games/screp/cmd/screp/R_IzaaP---_ReyAzucarZ_14240114.rep",
          OneVsOne(
            winner = SCPlayer("ReyAzucar", Zerg),
            loser = SCPlayer("I.zA.a", Protoss)
          )
        )
      )
      def assertParse(pathFile: String, result: SCGameMode): Unit = {
        val file = new File(pathFile)
        val parser =
          testKit.spawn(gameParseFactory.create(), s"parser-${pathFile.length}")
        val judger = testKit.spawn(GameJudge(), s"judger-${pathFile.length}")
        val probe =
          testKit.createTestProbe[SCGameMode](s"probe-${pathFile.length}")
        val wrapper = testKit.spawn(
          Behaviors.receive[GameInfo] { (_, message) =>
            message match {
              case rep: ReplayParsed => judger ! JudgeGame(rep, probe.ref)
              case ImpossibleToParse => probe.ref ! InvalidSCGameMode(Nil)
            }

            Behaviors.same
          },
          s"wrapper-${pathFile.length}"
        )

        parser ! GameParser.ReplayToParse(file, wrapper)
        probe.expectMessage(result)
      }

      input.foreach {
        case (path, result) => assertParse(path, result)
      }
    }
  }
}
