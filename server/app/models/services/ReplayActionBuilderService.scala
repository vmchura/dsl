package models.services

import shared.models.ChallongeOneVsOneMatchGameResult

import java.io.File
import scala.concurrent.Future

trait ReplayActionBuilderService {

  def parseReplayService: ParseReplayFileService

  def parseFileAndBuildAction(
      file: File,
      discordUserID1: String,
      discordUserID2: String,
      checkFileDuplicity: Boolean = true
  ): Future[Either[String, ChallongeOneVsOneMatchGameResult]]

}
