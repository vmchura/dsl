package modules

import com.google.inject.AbstractModule
import models.services.{DropBoxFilesService, DropBoxFilesServiceEmptyImpl, DropBoxFilesServiceImpl}
import net.codingwell.scalaguice.ScalaModule

class DropboxModule  extends AbstractModule with ScalaModule {
  /**
   * Usar DropBoxFilesServiceEmptyImpl si no se quiere agregar un archivo al dropbox
   * Usar DropBoxFilesServiceImpl si se quiere agregar un archivo al dropbox
   */
  override def configure(): Unit = {
    //bind[DropBoxFilesService].to[DropBoxFilesServiceEmptyImpl]
    bind[DropBoxFilesService].to[DropBoxFilesServiceImpl]
  }


}
