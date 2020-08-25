package modules

import com.google.inject.AbstractModule
import models.services.{S3FilesService, S3FilesServiceImpl}
import net.codingwell.scalaguice.ScalaModule

class S3Module extends AbstractModule with ScalaModule {

  override def configure(): Unit = {
    bind[S3FilesService].to[S3FilesServiceImpl]
  }


}
