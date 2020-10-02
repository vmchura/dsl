package models.daos
import models.{DiscordID, DiscordUserData, GuildID}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Span}
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import eu.timepit.refined.auto._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Random
import language.postfixOps
class UserHistoryDAOTest extends PlaySpec with GuiceOneAppPerSuite with ScalaFutures {
  implicit override val patienceConfig: PatienceConfig =
    PatienceConfig(timeout =  Span(10, Seconds), interval = Span(1, Seconds))
  val userHistoryDAO: UserHistoryDAO = app.injector.instanceOf(classOf[UserHistoryDAO])
  "UserHistoryDAO DAO" should {
    "load correct values" in {
      val id = DiscordID(Random.nextString(12))
      val guildID = GuildID(Random.nextString(12))
      val newUserName = Random.nextString(12)
      val er = for{
        noResult <- userHistoryDAO.load(id)
        insertion <- userHistoryDAO.updateWithLastInformation(id,guildID,DiscordUserData(id,newUserName, "1234",None))
        withResult <- userHistoryDAO.load(id)
      }yield{
        (noResult,insertion) match {
          case (None,true) => withResult
          case _ => None
        }
      }
      whenReady(er){ r =>
        assertResult(Some(newUserName))(r.map(_.lastUserName))
      }
    }
    "update correct values" in {
      val id = DiscordID(Random.nextString(12))

      val oldUserName = Random.nextString(12)
      val newUserName = Random.nextString(12)
      val guildID = GuildID(Random.nextString(12))
      val er = for{
        _ <- userHistoryDAO.updateWithLastInformation(id,guildID,DiscordUserData(id,oldUserName, "1234",None))
        _ <- userHistoryDAO.updateWithLastInformation(id,guildID,DiscordUserData(id,newUserName, "1234",None))
        withResult <- userHistoryDAO.load(id)
      }yield{
        withResult
      }
      whenReady(er){ r =>
        assertResult(Some(newUserName))(r.map(_.lastUserName))
        assertResult(Some(Seq(oldUserName,newUserName)))(r.map(_.logs.map(_.userName)))
      }
    }
  }
}
