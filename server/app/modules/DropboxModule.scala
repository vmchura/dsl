package modules

import com.google.inject.AbstractModule
import models.services.{DropBoxFilesService, DropBoxFilesServiceImpl}
import net.codingwell.scalaguice.ScalaModule

class DropboxModule  extends AbstractModule with ScalaModule {

  override def configure(): Unit = {
    bind[DropBoxFilesService].to[DropBoxFilesServiceImpl]
  }


}
