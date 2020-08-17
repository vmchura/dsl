package models.services

import java.io.File
import java.util.UUID

import models.MatchNameReplay
import org.scalatest.FunSuite
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.concurrent.ExecutionContext.Implicits.global

class DiscordFileServiceImplTest extends  PlaySpec with GuiceOneAppPerSuite{
  val service: DiscordFileService = app.injector.instanceOf(classOf[DiscordFileService])

  "A Discord File Service" should {
    "add file" in {
      val f = service.pushFileOnChannel("728442814832312375",new File("/home/vmchura/Games/starcraft-remastered/drive_c/users/vmchura/My Documents/StarCraft/Maps/Replays/BisuVsProts/Stork vs Bisu blue.rep"),"comment","fileName")
      Await.result(f,20.seconds)
      f.map(j => assert(j))
      //service.push(new File("/home/vmchura/Games/starcraft-remastered/drive_c/users/vmchura/My Documents/StarCraft/Maps/Replays/BisuVsProts/Stork vs Bisu blue.rep"),MatchNameReplay(UUID.randomUUID(),"1 D","TX","VmChQ","Trebol"))
    }

  }
}
