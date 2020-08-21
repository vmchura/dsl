package modules

import com.google.inject.AbstractModule
import models.services.{S3FilesService, S3FilesServiceEmptyImpl}
import net.codingwell.scalaguice.ScalaModule

class S3Module extends AbstractModule with ScalaModule {
  /**
   * Usar S3FilesServiceEmptyImpl si no se quiere agregar ningún archivo al AWS S3
   * Usar S3FilesServiceImpl si se quiere agregar un archivo al AWS S3
   */
  override def configure(): Unit = {
    bind[S3FilesService].to[S3FilesServiceEmptyImpl]
    //bind[S3FilesService].to[S3FilesServiceImpl]
  }


}
