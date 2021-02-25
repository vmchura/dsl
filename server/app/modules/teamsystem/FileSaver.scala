package modules.teamsystem

import java.io.File
import scala.concurrent.Future

trait FileSaver {
  def push(file: File, replayName: String): Future[Boolean]
}
