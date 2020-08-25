package modules

import com.google.inject.AbstractModule
import models.services.{ParseReplayFileService, ParseReplayFileServiceEmptyImpl}
import net.codingwell.scalaguice.ScalaModule

class ParseReplayFileModuleEmpty extends AbstractModule with ScalaModule {

  override def configure(): Unit = {
    bind[ParseReplayFileService].to[ParseReplayFileServiceEmptyImpl]
  }


}
