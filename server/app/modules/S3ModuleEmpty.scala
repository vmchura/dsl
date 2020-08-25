package modules

import com.google.inject.AbstractModule
import models.services.{S3FilesService, S3FilesServiceEmptyImpl}
import net.codingwell.scalaguice.ScalaModule

class S3ModuleEmpty extends AbstractModule with ScalaModule {

  override def configure(): Unit = {
    bind[S3FilesService].to[S3FilesServiceEmptyImpl]
  }


}
