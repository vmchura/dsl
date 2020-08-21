package models.services

import java.io.File
import java.util.UUID

import models.MatchNameReplay
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
class DropBoxFilesServiceImplTest extends  PlaySpec with GuiceOneAppPerSuite{
  val service: DropBoxFilesService = app.injector.instanceOf(classOf[DropBoxFilesService])

  "A DropBox Service" should {
    "add file" in {
      service.push(new File("/home/vmchura/Games/starcraft-remastered/drive_c/users/vmchura/My Documents/StarCraft/Maps/Replays/BisuVsProts/Stork vs Bisu blue.rep"),MatchNameReplay(UUID.randomUUID(),"1 D","TX","VmChQ","Trebol","-"))
    }

  }
}
