package models.services
import java.io.File
import java.util.UUID

import models.MatchNameReplay

import scala.concurrent.Future
import play.api.libs.Files.SingletonTemporaryFileCreator

class DropBoxFilesServiceEmptyImpl extends DropBoxFilesService {
  override protected def dropBoxAccessToken: String = ""

  override def push(file: File, replayName: MatchNameReplay): Future[Boolean] = Future.successful(true)

  override def download(replayID: UUID, fileName: String): Future[File] = {

    val file = SingletonTemporaryFileCreator.create("replay",".rep").toFile
    Future.successful(file)
  }

  override def delete(pathFileOnCloud: String): Future[Boolean] = Future.successful(true)

  override def delete(replayID: UUID): Future[Boolean] = Future.successful(true)

  override def wrapIntoFolder(currentPathFileOnCloud: String, folder: String): Future[Option[String]] = Future.successful{
    println(s"The file $currentPathFileOnCloud it will be wrapped into $folder")
    Some("/*/")
  }

  override def createFoldersAt(pathParent: String, folders: Seq[String]): Future[Boolean] = Future.successful{
    folders.foreach(f => println(s"Creating folder: $pathParent/$f"))
    true
  }
}
