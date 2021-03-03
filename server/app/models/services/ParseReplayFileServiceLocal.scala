package models.services

import java.io.File
import scala.concurrent.Future

class ParseReplayFileServiceLocal extends ParseReplayFileService {
  def parseFile(file: File): Future[Either[String, String]] = {
    import sys.process._
    val buffer = new StringBuilder()
    val path = "\"" + { file.getAbsolutePath } + "\""
    val res =
      s"/home/vmchura/Games/screp/cmd/screp/screp $path"
        .!(ProcessLogger(line => buffer.append(line)))
    if (res == 0) {
      Future.successful(Right(buffer.toString()))
    } else {

      Future.failed(
        new IllegalArgumentException("screp not installed correctly")
      )
    }
  }
}
