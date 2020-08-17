package models.services

import java.io.File

import awscala._
import s3._
import javax.inject.Inject
import models.MatchNameReplay
import play.api.Configuration
import scala.concurrent.Future

class S3FilesService @Inject()(configuration: Configuration) {


  implicit val s3: S3 = S3.at(Region.Ohio)
  def push(file: File, replayName: MatchNameReplay): Future[Boolean] = {


    s3.bucket("dsl-replays").fold(Future.successful(false)){ bucket =>
      try {
        bucket.put(replayName.uniqueNameReplay, file)
        Future.successful(true)
      }catch{
        case _: Throwable => Future.successful(false)
      }
    }
  }

}
