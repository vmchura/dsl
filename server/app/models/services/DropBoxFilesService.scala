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

class DropBoxFilesService @Inject()(configuration: Configuration) {

  private val dropBoxAccessToken: String = configuration.get[String]("dropbox.accessToken")

  def push(file: File, replayName: MatchNameReplay): Future[Boolean] = {
    import com.dropbox.core.DbxRequestConfig
    import com.dropbox.core.v2.DbxClientV2
    val config = DbxRequestConfig.newBuilder("dropbox/dsl-replays").build
    val client = new DbxClientV2(config, dropBoxAccessToken)
    Future{
      try {
        val in = new FileInputStream(file)

        client.files().uploadBuilder(s"/replays-ordered/${replayName.toString}").uploadAndFinish(in)

        in.close()
        true
      }catch {
        case _: Throwable => false
      }
    }
  }

}
