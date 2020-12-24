package modules.usertrajectory

import com.google.inject.AbstractModule
import models.daos.usertrajectory.{
  ReplayRecordResumenDAO,
  ReplayRecordResumenDAOImpl
}
import models.daos.{AuthTokenDAO, AuthTokenDAOImpl}
import models.services.{AuthTokenService, AuthTokenServiceImpl}
import net.codingwell.scalaguice.ScalaModule
import play.api.libs.concurrent.AkkaGuiceSupport

/**
  * The base Guice module.
  */
class UserTrajectoryModule
    extends AbstractModule
    with ScalaModule
    with AkkaGuiceSupport {

  /**
    * Configures the module.
    */
  override def configure(): Unit = {
    bind[ReplayRecordResumenDAO].to[ReplayRecordResumenDAOImpl]
    bindTypedActor(
      GameReplayResumenManager,
      "game-replay-resumen-manager-actor"
    )

  }
}
