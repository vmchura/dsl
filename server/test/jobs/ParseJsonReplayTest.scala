package jobs

import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

import scala.language.postfixOps

class ParseJsonReplayTest extends PlaySpec with GuiceOneAppPerSuite{
  val fileParser: ParseFile = app.injector.instanceOf(classOf[ParseFile])

  "Parse Json Replay" should {
    "get something" in {
      val x = fileParser.parseJsonResponse("{\"Header\":\n\t{\"Engine\":\n\t\t{\"Name\":\"Brood War\",\"ID\":1,\"ShortName\":\"BW\"},\n\"Frames\":11568,\"StartTime\":\"2020-06-05T23:52:03Z\",\"Title\":\"sa1\",\n\"MapWidth\":128,\"MapHeight\":128,\"AvailSlotsCount\":2,\"Speed\":{\"Name\":\"Fastest\",\"ID\":6},\n\"Type\":{\"Name\":\"One on One\",\"ID\":4,\"ShortName\":\"1v1\"},\"SubType\":1,\"Host\":\"ash-Sabb4th\",\n\"Map\":\"\\u0003Neo Sylphid \\u00052.0\",\n\"Players\":[\n{\t\"SlotID\":1,\n\t\"ID\":0,\n\t\"Type\":{\"Name\":\"Human\",\"ID\":2},\n\t\"Race\":{\"Name\":\"Terran\",\"ID\":1,\"ShortName\":\"ran\",\"Letter\":84},\n\t\"Team\":1,\n\t\"Name\":\"ash-Sabb4th\",\n\t\"Color\":{\"Name\":\"Orange\",\"ID\":4,\"RGB\":16288788}\n},\n{\"SlotID\":2,\"ID\":1,\"Type\":{\"Name\":\"Human\",\"ID\":2},\n\"Race\":{\"Name\":\"Protoss\",\"ID\":2,\"ShortName\":\"toss\",\"Letter\":80},\"Team\":2,\"Name\":\"shafirru\",\n\"Color\":{\"Name\":\"White\",\"ID\":6,\"RGB\":13426896}}]},\"Commands\":null,\"MapData\":null,\n\"Computed\":{\"LeaveGameCmds\":null,\n\"ChatCmds\":[{\"Frame\":186,\"PlayerID\":1,\"Type\":{\"Name\":\"Chat\",\"ID\":92},\n\"SenderSlotID\":2,\"Message\":\"im not good at playing a lot of games in a row\"},{\"Frame\":285,\"PlayerID\":1,\n\"Type\":{\"Name\":\"Chat\",\"ID\":92},\"SenderSlotID\":1,\"Message\":\"yeah me too gotta do something with the wife\"},{\"Frame\":312,\"PlayerID\":1,\n\"Type\":{\"Name\":\"Chat\",\"ID\":92},\"SenderSlotID\":2,\"Message\":\"they get less and less productivf or me\"},{\"Frame\":599,\"PlayerID\":1,\n\"Type\":{\"Name\":\"Chat\",\"ID\":92},\"SenderSlotID\":2,\"Message\":\"its either the gf or i stop thinking as i get more tired\"},{\"Frame\":621,\"PlayerID\":1,\n\"Type\":{\"Name\":\"Chat\",\"ID\":92},\"SenderSlotID\":2,\"Message\":\"its weird\"},{\"Frame\":941,\"PlayerID\":1,\n\"Type\":{\"Name\":\"Chat\",\"ID\":92},\"SenderSlotID\":1,\"Message\":\"hmm yeah i get tired too\"},{\"Frame\":1126,\"PlayerID\":1,\n\"Type\":{\"Name\":\"Chat\",\"ID\":92},\"SenderSlotID\":1,\"Message\":\"so i keep going sometimes\"},{\"Frame\":1198,\"PlayerID\":1,\n\"Type\":{\"Name\":\"Chat\",\"ID\":92},\"SenderSlotID\":1,\"Message\":\"cause i learn more when tired\"},{\"Frame\":1400,\"PlayerID\":1,\n\"Type\":{\"Name\":\"Chat\",\"ID\":92},\"SenderSlotID\":2,\"Message\":\"i play more lazy when im tired\"},{\"Frame\":1515,\"PlayerID\":1,\n\"Type\":{\"Name\":\"Chat\",\"ID\":92},\"SenderSlotID\":2,\"Message\":\"so i make mistakes that i already know not to make\"},{\"Frame\":11556,\"PlayerID\":1,\n\"Type\":{\"Name\":\"Chat\",\"ID\":92},\"SenderSlotID\":2,\"Message\":\"gg\"}],\n\"WinnerTeam\":1,\n\"PlayerDescs\":\n[\n{\"PlayerID\":0,\"LastCmdFrame\":11566,\"CmdCount\":1956,\"APM\":242,\"EffectiveCmdCount\":1395,\"EAPM\":172,\"StartLocation\":{\"X\":288,\"Y\":2960},\"StartDirection\":8},\n{\"PlayerID\":1,\"LastCmdFrame\":11556,\"CmdCount\":1615,\"APM\":200,\"EffectiveCmdCount\":1219,\"EAPM\":151,\"StartLocation\":{\"X\":3808,\"Y\":3184},\"StartDirection\":4}]}}")
      val replayParsed = x match {
        case Left(exception) => fail(exception)
        case Right(replay) => replay
      }

      assert(replayParsed.winner == 1 || replayParsed.winner == 2)
      assert(replayParsed.player1.nonEmpty)
      assert(replayParsed.player2.nonEmpty)
    }
  }


}
