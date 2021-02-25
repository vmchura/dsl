package modules.teamsystem
import java.io.File
import scala.concurrent.Future

import awscala._
import s3._
import scala.concurrent.ExecutionContext.Implicits.global
class FileSaverImpl extends FileSaver {

  implicit val s3: S3 = S3.at(Region.Ohio)
  override def push(file: File, replayName: String): Future[Boolean] =
    Future {
      s3.bucket("dsl-replays").fold(false) { bucket =>
        bucket.put(replayName, file)
        true
      }
    }.fallbackTo(Future.successful(false))
}
