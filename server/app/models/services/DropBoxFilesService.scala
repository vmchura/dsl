package models.services


import javax.inject.Inject
import play.api.Configuration
import java.io.{File, FileInputStream}
import java.util.UUID

import models.MatchNameReplay
import play.api.libs.Files
import play.api.mvc.MultipartFormData

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.v2.DbxClientV2
import play.api.libs.Files.SingletonTemporaryFileCreator
class DropBoxFilesService @Inject()(configuration: Configuration) {

  private val dropBoxAccessToken: String = configuration.get[String]("dropbox.accessToken")

  def push(file: File, replayName: MatchNameReplay): Future[Boolean] = {

    val config = DbxRequestConfig.newBuilder("dropbox/dsl-replays").build
    val client = new DbxClientV2(config, dropBoxAccessToken)
    Future{
      try {
        val in = new FileInputStream(file)

        client.files().uploadBuilder(s"${replayName.pathFileOnCloud}").uploadAndFinish(in)

        in.close()
        val in2 = new FileInputStream(file)

        client.files().uploadBuilder(s"/all/${replayName.uniqueNameReplay}").uploadAndFinish(in2)

        in2.close()
        true
      }catch {
        case _: Throwable => false
      }
    }
  }
  def download(replayID: UUID,fileName: String): Future[File] = {
    val config = DbxRequestConfig.newBuilder("dropbox/dsl-replays").build
    val client = new DbxClientV2(config, dropBoxAccessToken)

    try{
      import java.io.FileOutputStream
      Future {
        val file = SingletonTemporaryFileCreator.create(fileName).toFile
        val out = new FileOutputStream(file)

        client.files().downloadBuilder(s"/all/R_$replayID.rep").download(out)
        out.close()

        file
      }
    }catch {
      case e :Throwable => Future.failed(e)
    }
  }

}
