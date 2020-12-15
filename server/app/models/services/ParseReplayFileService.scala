package models.services

import java.io.File

import scala.concurrent.Future

trait ParseReplayFileService {
  def parseFile(file: File): Future[Either[String, String]]

}
