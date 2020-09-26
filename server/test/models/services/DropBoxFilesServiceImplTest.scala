package models.services

import java.io.File
import java.util.UUID

import models.MatchNameReplay
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.concurrent.ExecutionContext.Implicits.global
class DropBoxFilesServiceImplTest extends  PlaySpec with GuiceOneAppPerSuite{
  val service: DropBoxFilesService = app.injector.instanceOf(classOf[DropBoxFilesService])

  "A DropBox Service" should {
    "add file" in {
      service.push(new File("/home/vmchura/Games/starcraft-remastered/drive_c/users/vmchura/My Documents/StarCraft/Maps/Replays/BisuVsProts/Stork vs Bisu blue.rep"),MatchNameReplay(UUID.randomUUID(),"1 D","TX","VmChQ","Trebol","-"))
    }
    "wrap file" in {

      val u = service.wrapIntoFolder("/From/gg.txt","Game1")
      Await.result(u,20.seconds)
      u.map{ r =>
        assert(r.nonEmpty)
      }
    }
    "create folders" in {
      val u = service.createFoldersAt("/From/",Seq("Game1","Game2","Game3"))
      Await.result(u,20.seconds)
      u.map{ r =>
        assert(r)
      }
    }

  }
}
