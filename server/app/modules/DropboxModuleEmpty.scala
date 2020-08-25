package modules

import com.google.inject.AbstractModule
import models.services.{DropBoxFilesService, DropBoxFilesServiceEmptyImpl}
import net.codingwell.scalaguice.ScalaModule

class DropboxModuleEmpty  extends AbstractModule with ScalaModule {

  override def configure(): Unit = {
    bind[DropBoxFilesService].to[DropBoxFilesServiceEmptyImpl]
  }


}
