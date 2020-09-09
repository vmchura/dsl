package models

import java.io.File
import java.util.UUID

import models.{DiscordUser, MatchPK, MatchSmurf}
import models.daos.UserSmurfDAO
import models.services.ParseReplayFileService
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import shared.models.ActionByReplay

import scala.concurrent.Await
import scala.language.postfixOps
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt

class ReplayRecordTest extends  PlaySpec with GuiceOneAppPerSuite {


  "HashingFile" should {
    "get good result" in {
      val file = new File("/home/vmchura/Dropbox/Aplicaciones/dsl-replays/DCSL-REDEMPTION/Bracket/R1/R_player1-_player2-_10950014.rep")
      val hash1 = ReplayRecord.md5HashString(file)
      val hash2 = ReplayRecord.md5HashString(file)
      assert(hash1.equals(hash2))
    }
  }
}
