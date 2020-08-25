package modules

import com.google.inject.AbstractModule
import models.services.{DiscordFileService, DiscordFileServiceImpl}
import net.codingwell.scalaguice.ScalaModule

class DiscordFileModule  extends AbstractModule with ScalaModule {

  override def configure(): Unit = {
    bind[DiscordFileService].to[DiscordFileServiceImpl]
  }


}
