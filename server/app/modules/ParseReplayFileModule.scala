package modules

import com.google.inject.AbstractModule
import models.services.{ParseReplayFileService, ParseReplayFileServiceEmptyImpl, ParseReplayFileServiceImpl}
import net.codingwell.scalaguice.ScalaModule

class ParseReplayFileModule extends AbstractModule with ScalaModule {
  /**
   * Usar ParseReplayFileServiceEmptyImpl si no se requiere procesar el archivo de la replay
   * Usar ParseReplayFileServiceImpl si se quiere procesar el archivo de la replay
   */
  override def configure(): Unit = {
    //bind[ParseReplayFileService].to[ParseReplayFileServiceEmptyImpl]
    bind[ParseReplayFileService].to[ParseReplayFileServiceImpl]
  }


}
