package modules

import com.google.inject.AbstractModule
import models.daos.{TrovoUserDAO, TrovoUserDAOImpl}
import models.services.{UserTrovoService, UserTrovoServiceImpl}
import net.codingwell.scalaguice.ScalaModule

class TrovoModule extends AbstractModule with ScalaModule {

  /**
    * Configures the module.
    */
  override def configure(): Unit = {
    bind[TrovoUserDAO].to[TrovoUserDAOImpl]
    bind[UserTrovoService].to[UserTrovoServiceImpl]
  }

}
