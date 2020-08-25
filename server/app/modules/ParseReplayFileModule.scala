package modules

import com.google.inject.AbstractModule
import models.services.{ParseReplayFileService, ParseReplayFileServiceImpl}
import net.codingwell.scalaguice.ScalaModule

class ParseReplayFileModule extends AbstractModule with ScalaModule {

  override def configure(): Unit = {
    bind[ParseReplayFileService].to[ParseReplayFileServiceImpl]
  }


}
