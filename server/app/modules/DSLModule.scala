package modules

import com.google.inject.AbstractModule
import models.daos.{ParticipantDAO, ParticipantDAOImpl}
import models.services.{ParticipantsService, ParticipantsServiceImpl}
import net.codingwell.scalaguice.ScalaModule

/**
 * The base Guice module.
 */
class DSLModule extends AbstractModule with ScalaModule {

  /**
   * Configures the module.
   */
  override def configure(): Unit = {
    bind[ParticipantsService].to[ParticipantsServiceImpl]
    bind[ParticipantDAO].to[ParticipantDAOImpl]
  }
}
