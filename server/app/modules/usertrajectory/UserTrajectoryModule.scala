package modules.usertrajectory

import com.google.inject.AbstractModule
import models.daos.usertrajectory.{
  ReplayRecordResumenDAO,
  ReplayRecordResumenDAOImpl
}
import models.daos.{AuthTokenDAO, AuthTokenDAOImpl}
import models.services.{AuthTokenService, AuthTokenServiceImpl}
import net.codingwell.scalaguice.ScalaModule

/**
  * The base Guice module.
  */
class UserTrajectoryModule extends AbstractModule with ScalaModule {

  /**
    * Configures the module.
    */
  override def configure(): Unit = {
    bind[ReplayRecordResumenDAO].to[ReplayRecordResumenDAOImpl]
  }
}
