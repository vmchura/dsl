package models.daos
import models.{DiscordID, GuildID}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Span}
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Random
class UserGuildDAOTest extends PlaySpec with GuiceOneAppPerSuite with ScalaFutures {
  implicit override val patienceConfig: PatienceConfig =
    PatienceConfig(timeout =  Span(10, Seconds), interval = Span(1, Seconds))

  val userGuildDAO: UserGuildDAO = app.injector.instanceOf(classOf[UserGuildDAO])

  "UserGuildDAO" should {
    "return empty set" in {
      whenReady(userGuildDAO.load(DiscordID(Random.nextString(12)))){ guilds =>
        assert(guilds.isEmpty)
      }
    }
    "return correct set after 2 insertions" in {
      val discordID = DiscordID(Random.nextString(12))
      val guildID_0 = GuildID(Random.nextString(5))
      val guildID_1 = GuildID(Random.nextString(5))
      val insertion = userGuildDAO.addGuildToUser(discordID,guildID_0).flatMap(_ => userGuildDAO.addGuildToUser(discordID,guildID_1))

      whenReady(insertion.flatMap(_ =>userGuildDAO.load(discordID))){ guilds =>
        assertResult(Set(guildID_0,guildID_1))(guilds)
      }
    }
    "return correct set after 2 insertions and 1 repetition" in {
      val discordID = DiscordID(Random.nextString(12))
      val guildID_0 = GuildID(Random.nextString(5))
      val guildID_1 = GuildID(Random.nextString(5))
      val insertion = for{
        _ <- userGuildDAO.addGuildToUser(discordID,guildID_0)
        _ <- userGuildDAO.addGuildToUser(discordID,guildID_1)
        _ <- userGuildDAO.addGuildToUser(discordID,guildID_0)
      }yield{
        true
      }

      whenReady(insertion.flatMap(_ =>userGuildDAO.load(discordID))){ guilds =>
        assertResult(Set(guildID_0,guildID_1))(guilds)
      }
    }
  }
}
