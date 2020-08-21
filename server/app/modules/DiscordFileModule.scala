package modules

import com.google.inject.AbstractModule
import models.services.{DiscordFileService, DiscordFileServiceEmptyImpl}
import net.codingwell.scalaguice.ScalaModule

class DiscordFileModule  extends AbstractModule with ScalaModule {
  /**
   * Usar DiscordFileServiceEmptyImpl si no se quiere agregar un archivo al canal de discord
   * Usar DiscordFileServiceImpl si se quiere agregar un archivo al canal de discord
   */
  override def configure(): Unit = {
    bind[DiscordFileService].to[DiscordFileServiceEmptyImpl]
    //bind[DiscordFileService].to[DiscordFileServiceImpl]
  }


}
