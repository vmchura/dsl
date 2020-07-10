package models.services
import models.DiscordUser
import org.scalatest.AsyncFlatSpec
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import scala.concurrent.ExecutionContext.Implicits.global

class DiscordUserServiceImplTest  extends PlaySpec with GuiceOneAppPerSuite{
  val service: DiscordUserService = app.injector.instanceOf(classOf[DiscordUserService])

  "A Discord User Service" should {
    "get correct users" in {
      service.findMembersOnGuild("728442814832312372").map {
        users => assertResult(Seq(DiscordUser("698648718999814165", "VmChQ")))(users)
      }
    }

  }
}
