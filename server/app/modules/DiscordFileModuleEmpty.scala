package modules

import com.google.inject.AbstractModule
import models.services.{DiscordFileService, DiscordFileServiceEmptyImpl}
import net.codingwell.scalaguice.ScalaModule

class DiscordFileModuleEmpty  extends AbstractModule with ScalaModule {

  override def configure(): Unit = {
    bind[DiscordFileService].to[DiscordFileServiceEmptyImpl]
  }


}
