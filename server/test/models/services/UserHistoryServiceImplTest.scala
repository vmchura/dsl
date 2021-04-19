package models.services

import models.daos.UserSmurfDAO
import models.{DiscordUser, MatchPK, MatchSmurf}
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps

class UserHistoryServiceImplTest
    extends PlaySpec
    with GuiceOneAppPerSuite
    with ScalaFutures {
  val service: UserHistoryService =
    app.injector.instanceOf(classOf[UserHistoryService])

  "update users on valid but not history" in {
    val update = service.update()
    whenReady(update, Timeout(30 seconds)) { items =>
      assert(items > 0)
    }
  }

}
