package models.daos
import models.{DiscordID, Smurf}
import models.services.SmurfService
import models.services.SmurfService.SmurfAdditionResult
import org.scalatest.Assertion
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Span}
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Random
import language.postfixOps
import scala.concurrent.Future
class ValidUserSmurfDAOImplTest extends PlaySpec with GuiceOneAppPerSuite with ScalaFutures{
  implicit override val patienceConfig: PatienceConfig =
    PatienceConfig(timeout =  Span(10, Seconds), interval = Span(1, Seconds))

  val service: SmurfService = app.injector.instanceOf(classOf[SmurfService])
  "Smurf service" should {
    "no result of unknown user" in {
      val u = service.loadSmurfs(DiscordID(Random.nextString(12)))
      whenReady(u){ fv =>
        assert(fv.isEmpty)
      }
    }
    "result on insertion" in {
      val id = DiscordID(Random.nextString(12))
      val smurf = Smurf(Random.nextString(12))
      val insertion = service.addSmurf(id, smurf)
      val futResponse = for{
        _ <- service.addSmurf(id, smurf)
        loaded <- service.loadSmurfs(id)
      }yield{

        loaded
      }
      whenReady(insertion){ insertion =>
        assertResult(SmurfAdditionResult.Added)(insertion) }

      whenReady(futResponse){ fv =>
        assertResult(Some(Seq(smurf)))(fv.map(_.smurfs))
      }
    }
    "result valid response on insertions" in {
      val id = DiscordID(Random.nextString(12))
      val smurf = Smurf(Random.nextString(12))
      val validInsertion = service.addSmurf(id, smurf)
      val alreadyInserted = validInsertion.flatMap(_ => service.addSmurf(id, smurf))
      val alreadyHasOwner = validInsertion.flatMap(_ => service.addSmurf(DiscordID(Random.nextString(12)), smurf))
      def checkResult(result: Future[SmurfAdditionResult.AdditionResult],
                      expected: SmurfAdditionResult.AdditionResult): Assertion = {
        whenReady(result){ fv =>  assertResult(expected)(fv)}
      }

      checkResult(validInsertion, SmurfAdditionResult.Added)
      checkResult(alreadyInserted, SmurfAdditionResult.AlreadyRegistered)
      checkResult(alreadyHasOwner, SmurfAdditionResult.CantBeAdded)
    }
  }
}
