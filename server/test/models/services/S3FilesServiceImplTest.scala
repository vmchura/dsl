package models.services

import scala.concurrent.ExecutionContext.Implicits.global

import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

import scala.concurrent.Await
import scala.concurrent.duration._

class S3FilesServiceImplTest extends  PlaySpec with GuiceOneAppPerSuite{
  val service: S3FilesService = app.injector.instanceOf(classOf[S3FilesService])

  "A S3 Service" should {
    "add file" in {
      val res = service.push(null,null)
      Await.result(res,20.seconds)
      res.map(x => assert(x))
    }

  }
}
