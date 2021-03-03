package modules.teamsystem
import java.io.File
import scala.concurrent.Future

class FileSaverImplEmpty extends FileSaver {
  override def push(file: File, replayName: String): Future[Boolean] =
    Future.successful(true)
}
