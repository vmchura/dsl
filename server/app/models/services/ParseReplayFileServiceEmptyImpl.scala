package models.services

import java.io.File

import scala.concurrent.Future

class ParseReplayFileServiceEmptyImpl extends ParseReplayFileService {
  def parseFile(file: File): Future[Either[String, String]] =
    Future.successful(Left("NOT IMPLEMENTED FUNCTION"))
}
