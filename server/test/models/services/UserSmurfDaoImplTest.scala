package models.services

import java.util.UUID

import models.{DiscordUser, MatchPK, MatchSmurf}
import models.daos.UserSmurfDAO
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.language.postfixOps

class UserSmurfDaoImplTest extends PlaySpec with GuiceOneAppPerSuite{
  val dao: UserSmurfDAO = app.injector.instanceOf(classOf[UserSmurfDAO])


  "return no participant by smurf" in {
    val smurfTest = "randomSmurf"
    val queryExecution = for{
      participant <- dao.findBySmurf(smurfTest)
    }yield{
      assert(participant.isEmpty)
    }
    Await.result(queryExecution, 5 seconds)
    queryExecution

  }
  "add and delete smurf" in {
    val smurfTest = "randomSmurf"
    val du = DiscordUser("asd","xyz",Some("1234"))
    val matchSmurf = MatchSmurf(UUID.randomUUID(),MatchPK(0L,141L), smurfTest)
    val queryExecution = for{
      inserted      <- dao.addUser(du)
      smurfAdded    <- if(inserted) dao.addSmurf(du.discordID, matchSmurf) else Future.successful(false)
      withSmurf     <- dao.findUser(du.discordID)
      smurfRemoved  <- if(smurfAdded) dao.removeSmurf(du.discordID, matchSmurf) else Future.successful(false)
      withNoSmurf   <- dao.findUser(du.discordID)
      deleted       <- dao.removeUser(du.discordID)
    }yield{

      assert(deleted)
      assert(smurfRemoved)
      assert(withSmurf.fold(false)(_.matchSmurf.map(_.smurf).contains(smurfTest)))
      assert(withNoSmurf.fold(false)(! _.matchSmurf.map(_.smurf).contains(smurfTest)))
    }
    Await.result(queryExecution, 5 seconds)
    queryExecution
  }
  "find by smurf" in {
    val smurfTest = "randomSmurf"
    val du = DiscordUser("asd","xyz",Some("1234"))

    val queryExecution = for{
      inserted    <- dao.addUser(du)
      smurfAdded  <- if(inserted) dao.addSmurf(du.discordID, MatchSmurf(UUID.randomUUID(),MatchPK(0L,141L), smurfTest)) else Future.successful(false)
      withSmurf   <- dao.findBySmurf(smurfTest)
      deleted     <- dao.removeUser(du.discordID)
    }yield{
      assert(smurfAdded)
      assert(deleted)
      assertResult(List(du.discordID))(withSmurf.map(_.discordUser.discordID))
    }
    Await.result(queryExecution, 5 seconds)
    queryExecution

  }
}
