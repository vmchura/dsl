package models.daos.usertrajectory

import database.TemporalDB
import database.DataBaseObjects._
import models.daos.usertrajectory.ReplayRecordResumenDAO.ByPlayer
import models.{DiscordPlayer, ReplayRecordResumen, Smurf}
import models.services.ReplayActionBuilderService
import org.scalatest.time.{Seconds, Span}
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.play.PlaySpec
import play.api.libs.Files.TemporaryFileCreator
import shared.models.ChallongeOneVsOneMatchGameResult

import java.io.File
import java.util.UUID
class ReplayRecordResumenDAOTest
    extends PlaySpec
    with TemporalDB
    with AnyWordSpecLike {
  private val replayActionBuilderService =
    app.injector.instanceOf(classOf[ReplayActionBuilderService])
  private val replayRecordResumenDAO =
    app.injector.instanceOf(classOf[ReplayRecordResumenDAO])
  implicit override val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = Span(10, Seconds), interval = Span(1, Seconds))

  def initReplay(): Unit = {
    initTournament()
    initUser(first_user)
    initUser(second_user)
    addSmurfToUser(first_user, Smurf("G19"))
    addSmurfToUser(second_user, Smurf(".Chester"))
    val fileSource = new File(
      getClass.getResource("/G19Vs.Chester.rep").getPath
    )
    val fileResult = File.createTempFile("replay", ".rep")
    import java.nio.file.StandardCopyOption.REPLACE_EXISTING
    val tmpFileCreator =
      app.injector.instanceOf(classOf[TemporaryFileCreator])
    java.nio.file.Files
      .copy(fileSource.toPath, fileResult.toPath, REPLACE_EXISTING)
    val file =
      tmpFileCreator.create(
        fileResult.toPath
      )
    val response = replayActionBuilderService
      .parseFileAndBuildAction(
        file,
        first_user.loginInfo.providerKey,
        second_user.loginInfo.providerKey
      )
      .futureValue
    val toAdd: Option[ReplayRecordResumen] = {
      response.toOption.map {
        case ChallongeOneVsOneMatchGameResult(winner, loser) =>
          val discordWinner = DiscordPlayer(
            winner.discordID.toOption
              .map(_.withSource.buildDefined().discordIDValue),
            winner.player
          )
          val discordLoser = DiscordPlayer(
            loser.discordID.toOption
              .map(_.withSource.buildDefined().discordIDValue),
            loser.player
          )
          ReplayRecordResumen(
            UUID.randomUUID(),
            discordWinner,
            discordLoser,
            enabled = true
          )
      }

    }
    toAdd match {
      case Some(valueToAdd) =>
        replayRecordResumenDAO.add(valueToAdd).futureValue
      case None => fail("No value to add")
    }
  }

  "ReplayRecordResumenDAO" must {
    "Insert and load data" in {
      initReplay()
      val loaded = replayRecordResumenDAO
        .load(ByPlayer(first_user.loginInfo.providerKey))
        .futureValue
      assertResult(1)(loaded.length)
    }
  }
}
