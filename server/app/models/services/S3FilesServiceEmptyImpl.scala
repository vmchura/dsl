package models.services

import java.io.File

import models.MatchNameReplay

import scala.concurrent.Future

class S3FilesServiceEmptyImpl extends S3FilesService{


  def push(file: File, replayName: MatchNameReplay): Future[Boolean] = Future.successful(true)

}
