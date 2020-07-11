package modules

import com.google.inject.AbstractModule
import models.daos.{ParticipantDAO, ParticipantDAOImpl, TournamentDAO, TournamentDAOImpl}
import models.services.{ChallongeTournamentService, ChallongeTournamentServiceImpl, DiscordUserService, DiscordUserServiceImpl, ParticipantsService, ParticipantsServiceImpl, TournamentService, TournamentServiceImpl}
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

    bind[TournamentDAO].to[TournamentDAOImpl]
    bind[TournamentService].to[TournamentServiceImpl]

    bind[DiscordUserService].to[DiscordUserServiceImpl]
    bind[ChallongeTournamentService].to[ChallongeTournamentServiceImpl]
  }
}