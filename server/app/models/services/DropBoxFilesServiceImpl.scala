package models.services


import javax.inject.Inject
import play.api.Configuration
import java.io.{File, FileInputStream}
import java.util.UUID

import models.MatchNameReplay

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.v2.DbxClientV2
import play.api.libs.Files.SingletonTemporaryFileCreator
class DropBoxFilesServiceImpl @Inject()(configuration: Configuration) extends DropBoxFilesService {

  protected val dropBoxAccessToken: String = configuration.get[String]("dropbox.accessToken")

  private def buildDropBoxConfig() = DbxRequestConfig.newBuilder("dropbox/dsl-replays").build
  def withDBClient[A](runClient: DbxClientV2 => A): Future[A] = {
    val config = buildDropBoxConfig()
    val client = new DbxClientV2(config, dropBoxAccessToken)
    try{
      Future {
        runClient(client)
      }
    }catch {
      case e :Throwable => Future.failed(e)
    }
  }
  def push(file: File, replayName: MatchNameReplay): Future[Boolean] = {
    withDBClient{ client =>
      val in = new FileInputStream(file)

      client.files().uploadBuilder(s"${replayName.pathFileOnCloud}").uploadAndFinish(in)

      in.close()
      val in2 = new FileInputStream(file)

      client.files().uploadBuilder(s"/all/${replayName.uniqueNameReplay}").uploadAndFinish(in2)

      in2.close()
      true
    }
  }
  def download(replayID: UUID,fileName: String): Future[File] = {
    withDBClient{ client =>
      import java.io.FileOutputStream
      val file = SingletonTemporaryFileCreator.create(fileName).toFile
      val out = new FileOutputStream(file)

      client.files().downloadBuilder(s"/all/R_$replayID.rep").download(out)
      out.close()

      file
    }
  }


  private def deleteByPath(path: String): Future[Boolean] = {
      withDBClient{ client => {
          val res = client.files().deleteV2(path)
          res.getMetadata.getName.nonEmpty
        }
      }
  }
  def delete(pathFileOnCloud: String): Future[Boolean] = deleteByPath(s"$pathFileOnCloud")
  def delete(replayID: UUID): Future[Boolean] = deleteByPath(s"/all/R_$replayID.rep")

  def getFileName(path: String)(implicit clientV2: DbxClientV2): Option[String] = {
    try{
      val m = clientV2.files().getMetadata(path)
      Some(m.getName)
    }catch {
      case _: Throwable => None
    }
  }

  override def wrapIntoFolder(currentPathFileOnCloud: String, folder: String): Future[Option[String]] = {

   withDBClient { implicit client =>
     getFileName(currentPathFileOnCloud).fold(Option.empty[String]) { fileName =>
       val i = currentPathFileOnCloud.lastIndexOf(fileName)
       if(i > 0) {
         val destinyFileOnCloud = currentPathFileOnCloud.substring(0,i) + folder+"/"+fileName
         val metadata = client.files().moveV2(currentPathFileOnCloud, destinyFileOnCloud)
         if(metadata.getMetadata.getName.nonEmpty)
           Some(destinyFileOnCloud)
         else
           None
       }else{
         None
       }
     }


   }


  }

  override def createFoldersAt(pathParent: String, folders: Seq[String]): Future[Boolean] = withDBClient[Boolean]{ implicit client =>
    getFileName(pathParent).fold(false){ _ =>
      val folderWithNoSlash = if(pathParent.endsWith("/")) pathParent.dropRight(1) else pathParent
      folders.forall(f => {
        val newFolder = folderWithNoSlash+"/"+f
        if(getFileName(newFolder).isEmpty){
          val m = client.files().createFolderV2(newFolder)
          m.getMetadata.getName.nonEmpty
        }else{
          true
        }
      })

    }
  }
}
