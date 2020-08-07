package jobs

import java.io.File

import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

import scala.concurrent.Await
import scala.language.postfixOps
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt

class ParseFileTest extends PlaySpec with GuiceOneAppPerSuite{
  val fileParser: ParseFile = app.injector.instanceOf(classOf[ParseFile])

  "ParseFile" should {
    "get something" in {
      val file = new File("/home/vmchura/Games/starcraft-remastered/drive_c/users/vmchura/My Documents/StarCraft/Maps/Replays/ReplaysSaved-SC/dtfastexpand.rep")


      val bodyResponse = fileParser.parseFile(file).map{
        case Left(exception) => fail(exception)
        case Right(value) => value
      }

      val replay =  bodyResponse.map(br =>  fileParser.parseJsonResponse(br) match {
        case Left(exception) => fail(exception)
        case Right(replayParsed) => replayParsed
      })

      val execution = replay.map{ r =>
        assert(r.player1.nonEmpty)
        assert(r.player2.nonEmpty)
        assert(r.winner == 1 || r.winner == 2)
      }

      Await.result(execution, 5.seconds)

      execution


    }
  }


}
