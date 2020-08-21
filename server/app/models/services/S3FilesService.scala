package models.services

import java.io.File

import models.MatchNameReplay

import scala.concurrent.Future

trait S3FilesService {
  def push(file: File, replayName: MatchNameReplay): Future[Boolean]
}
