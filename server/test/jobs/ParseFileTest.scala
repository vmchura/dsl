package jobs

import java.io.File

import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

import scala.language.postfixOps
class ParseFileTest extends PlaySpec with GuiceOneAppPerSuite{
  val fileParser: ParseFile = app.injector.instanceOf(classOf[ParseFile])

  "ParseFile" should {
    "get something" in {
      val file = new File("/home/vmchura/Games/starcraft-remastered/drive_c/users/vmchura/My Documents/StarCraft/Maps/Replays/ReplaysSaved-SC/dtfastexpand.rep")

      val x = fileParser.parseFile(file)

      val bodyResponse = x match {
        case Left(exception) => fail(exception)
        case Right(value) => value
      }

      val replay =  fileParser.parseJsonResponse(bodyResponse) match {
        case Left(exception) => fail(exception)
        case Right(replayParsed) => replayParsed
      }

      assert(replay.player1.nonEmpty)
      assert(replay.player2.nonEmpty)
      assert(replay.winner.nonEmpty)
    }
  }


}
