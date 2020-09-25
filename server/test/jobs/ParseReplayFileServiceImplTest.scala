package jobs

import java.io.File
import java.util.UUID

import models.{DiscordUser, MatchPK, MatchSmurf}
import models.daos.UserSmurfDAO
import models.services.ParseReplayFileService
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import shared.models.ActionByReplay

import scala.concurrent.Await
import scala.language.postfixOps
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt

class ParseReplayFileServiceImplTest extends PlaySpec with GuiceOneAppPerSuite{
  val fileParser: ParseReplayFileService = app.injector.instanceOf(classOf[ParseReplayFileService])
  val userDAO: UserSmurfDAO = app.injector.instanceOf(classOf[UserSmurfDAO])

  "ParseFile" should {
    "get something" in {
      //val file = new File("/home/vmchura/Games/screp/cmd/screp/queen_kevin1.rep")
      val file = new File("/home/vmchura/Games/screp/cmd/screp/LastReplay.rep")


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
    "build action correctly - no users" in {
      import shared.models.ActionBySmurf._
      val file = new File("/home/vmchura/Games/starcraft-remastered/drive_c/users/vmchura/My Documents/StarCraft/Maps/Replays/ReplaysSaved-SC/dtfastexpand.rep")

      val u1 = DiscordUser("1","1Name",Some("1234"))
      val u2 = DiscordUser("2","2Name",Some("1234"))
      val execution = for{
        i1 <- userDAO.addUser(u1)
        i2 <- userDAO.addUser(u2)
        action <- fileParser.parseFileAndBuildAction(file,u1.discordID,u2.discordID)
        d1 <- userDAO.removeUser(u1.discordID)
        d2 <- userDAO.removeUser(u2.discordID)
      }yield{
        assert(i1 && i2 && d1 && d2)
        println(action)
        val actionMade = action match {
          case Right(ActionByReplay(_,_,_, actionToTake, _,_)) => Some(actionToTake)
          case _ => None
        }
        assertResult(Some(SmurfsEmpty))(actionMade)
      }

      Await.result(execution, 5.seconds)

      execution

    }

    "build action correctly - only 1 user" in {
      import shared.models.ActionBySmurf._
      val file = new File("/home/vmchura/Games/starcraft-remastered/drive_c/users/vmchura/My Documents/StarCraft/Maps/Replays/ReplaysSaved-SC/dtfastexpand.rep")

      val u1 = DiscordUser("1","1Name",Some("1234"))
      val u2 = DiscordUser("2","2Name",Some("1234"))
      val execution = for{
        i1 <- userDAO.addUser(u1)
        i2 <- userDAO.addUser(u2)
        i3 <- userDAO.addSmurf(u1.discordID,MatchSmurf(UUID.randomUUID(),MatchPK(0L,0L),"shafirru"))
        action <- fileParser.parseFileAndBuildAction(file,u1.discordID,u2.discordID)
        d1 <- userDAO.removeUser(u1.discordID)
        d2 <- userDAO.removeUser(u2.discordID)
      }yield{
        assert(i1 && i2 && d1 && d2 && i3)
        println(action)
        val actionMade = action match {
          case Right(ActionByReplay(_,_,_, actionToTake, _,_)) => Some(actionToTake)
          case _ => None
        }
        assertResult(Some(Correlated1d2rDefined))(actionMade)
      }

      Await.result(execution, 5.seconds)

      execution

    }
    "build action correctly - 2 user" in {
      import shared.models.ActionBySmurf._
      val file = new File("/home/vmchura/Games/starcraft-remastered/drive_c/users/vmchura/My Documents/StarCraft/Maps/Replays/ReplaysSaved-SC/dtfastexpand.rep")

      val u1 = DiscordUser("1","1Name",Some("1234"))
      val u2 = DiscordUser("2","2Name",Some("1234"))
      val execution = for{
        i1 <- userDAO.addUser(u1)
        i2 <- userDAO.addUser(u2)
        i3 <- userDAO.addSmurf(u1.discordID,MatchSmurf(UUID.randomUUID(),MatchPK(0L,0L),"shafirru"))
        i4 <- userDAO.addSmurf(u2.discordID,MatchSmurf(UUID.randomUUID(),MatchPK(0L,0L),"ash-Sabb4th"))
        action <- fileParser.parseFileAndBuildAction(file,u1.discordID,u2.discordID)
        d1 <- userDAO.removeUser(u1.discordID)
        d2 <- userDAO.removeUser(u2.discordID)
      }yield{
        assert(i1 && i2 && d1 && d2 && i3 && i4)
        println(action)
        val actionMade = action match {
          case Right(ActionByReplay(_,_,_, actionToTake, _,_)) => Some(actionToTake)
          case _ => None
        }
        assertResult(Some(CorrelatedCruzadoDefined))(actionMade)
      }

      Await.result(execution, 20.seconds)

      execution

    }
    "build action correctly - 3 user" in {
      import shared.models.ActionBySmurf._
      val file = new File("/home/vmchura/Games/starcraft-remastered/drive_c/users/vmchura/My Documents/StarCraft/Maps/Replays/ReplaysSaved-SC/dtfastexpand.rep")

      val u1 = DiscordUser("1","1Name",Some("1234"))
      val u2 = DiscordUser("2","2Name",Some("1234"))
      val u3 = DiscordUser("3","3Name",Some("1234"))
      val execution = for{
        i1 <- userDAO.addUser(u1)
        i2 <- userDAO.addUser(u2)
        ix <- userDAO.addUser(u3)
        i3 <- userDAO.addSmurf(u1.discordID,MatchSmurf(UUID.randomUUID(),MatchPK(0L,0L),"shafirru"))
        i4 <- userDAO.addSmurf(u3.discordID,MatchSmurf(UUID.randomUUID(),MatchPK(0L,0L),"ash-Sabb4th"))
        action <- fileParser.parseFileAndBuildAction(file,u1.discordID,u2.discordID)
        d1 <- userDAO.removeUser(u1.discordID)
        d2 <- userDAO.removeUser(u2.discordID)
        d3 <- userDAO.removeUser(u3.discordID)
      }yield{
        assert(i1 && i2 && d1 && d2 && i3 && i4 && ix && d3)
        println(action)
        val actionMade = action match {
          case Right(ActionByReplay(_,_,_, actionToTake, _,_)) => Some(actionToTake)
          case _ => None
        }
        assertResult(Some(ImpossibleToDefine))(actionMade)
      }

      Await.result(execution, 20.seconds)

      execution

    }
  }


}
