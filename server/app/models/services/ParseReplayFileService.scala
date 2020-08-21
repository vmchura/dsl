package models.services

import java.io.File

import shared.models.{ActionByReplay, ReplayDescriptionShared}

import scala.concurrent.Future

trait ParseReplayFileService {
  def parseFile(file: File): Future[Either[String,String]]
  def parseJsonResponse(stringJson: String): Either[String,ReplayDescriptionShared]
  def parseFileAndBuildAction(file: File, discordUserID1: String, discordUserID2: String): Future[Either[String,ActionByReplay]]
}
