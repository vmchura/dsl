package modules.teamsystem

import com.google.inject.AbstractModule
import models.daos.teamsystem.{TeamDAO, TeamDAOImpl}
import net.codingwell.scalaguice.ScalaModule

class TeamSystemModule extends AbstractModule with ScalaModule {
  override def configure(): Unit = {
    bind[TeamDAO].to[TeamDAOImpl]
  }
}
