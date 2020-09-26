package models.services

import java.io.File
import java.util.UUID

import models.MatchNameReplay

import scala.concurrent.Future

trait DropBoxFilesService {
  protected def dropBoxAccessToken: String
  def push(file: File, replayName: MatchNameReplay): Future[Boolean]
  def download(replayID: UUID,fileName: String): Future[File]
  def delete(pathFileOnCloud: String): Future[Boolean]
  def delete(replayID: UUID): Future[Boolean]
  def wrapIntoFolder(currentPathFileOnCloud: String, folder: String): Future[Option[String]]
  def createFoldersAt(pathParent: String, folders:Seq[String]): Future[Boolean]
}
