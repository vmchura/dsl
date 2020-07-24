package models.services
import models.DiscordUser
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.postfixOps
import scala.concurrent.duration._
class DiscordUserServiceImplTest  extends PlaySpec with GuiceOneAppPerSuite{
  val service: DiscordUserService = app.injector.instanceOf(classOf[DiscordUserService])

  "A Discord User Service" should {
    "get correct users" in {
      val queryExecution = service.findMembersOnGuild("736004357866389584").map {
        case Some(users) => assert(users.contains(DiscordUser("698648718999814165", "VmChQ")))
        case None => fail()
      }
      Await.result(queryExecution, 30 seconds)

      queryExecution
    }

  }
}
