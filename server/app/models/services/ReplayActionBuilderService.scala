package models.services

import jobs.{JobError, UnknowReplayPusherError}
import shared.models.{ChallongeOneVsOneMatchGameResult, ReplayDescriptionShared}

import java.io.File
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

trait ReplayActionBuilderService {

  def parseReplayService: ParseReplayFileService

  def parseFileAndBuildAction(
      file: File,
      discordUserID1: String,
      discordUserID2: String
  ): Future[Either[String, ChallongeOneVsOneMatchGameResult]]

  def parseFileAndBuildDescription(
      file: File
  ): Future[Either[JobError, ReplayDescriptionShared]] = {
    (for {
      parsedEither <- parseReplayService.parseFile(file)
    } yield {
      parsedEither.flatMap(parseReplayService.parseJsonResponse)
    }).map {
      case Left(error) => Left(UnknowReplayPusherError(error))
      case Right(x)    => Right(x)
    }
  }
}
