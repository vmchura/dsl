package modules

import com.google.inject.AbstractModule
import models.daos.{
  MatchResultDAO,
  MatchResultDAOImpl,
  ParticipantDAO,
  ParticipantDAOImpl,
  ReplayMatchDAO,
  ReplayMatchDAOImpl,
  TickerReplayDAOImpl,
  TicketReplayDAO,
  TournamentDAO,
  TournamentDAOImpl,
  UserGuildDAO,
  UserGuildDAOImpl,
  UserHistoryDAO,
  UserHistoryDAOImpl,
  UserLeftGuildDAO,
  UserLeftGuildDAOImpl,
  UserSmurfDAO,
  UserSmurfDAOImpl,
  ValidUserSmurfDAO,
  ValidUserSmurfDAOImpl
}
import models.services.{
  ChallongeTournamentService,
  ChallongeTournamentServiceImpl,
  DiscordUserService,
  DiscordUserServiceImpl,
  ParticipantsService,
  ParticipantsServiceImpl,
  ReplayActionBuilderService,
  ReplayActionBuilderServiceImpl,
  ReplayDeleterService,
  ReplayDeleterServiceImpl,
  SmurfService,
  SmurfServiceImpl,
  TournamentSeriesService,
  TournamentSeriesServiceImpl,
  TournamentService,
  TournamentServiceImpl,
  UserHistoryService,
  UserHistoryServiceImpl
}
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

    bind[ReplayMatchDAO].to[ReplayMatchDAOImpl]
    bind[MatchResultDAO].to[MatchResultDAOImpl]
    bind[UserSmurfDAO].to[UserSmurfDAOImpl]
    bind[SmurfService].to[SmurfServiceImpl]
    bind[ReplayDeleterService].to[ReplayDeleterServiceImpl]

    bind[TicketReplayDAO].to[TickerReplayDAOImpl]

    bind[ValidUserSmurfDAO].to[ValidUserSmurfDAOImpl]
    bind[UserGuildDAO].to[UserGuildDAOImpl]
    bind[UserHistoryDAO].to[UserHistoryDAOImpl]
    bind[UserHistoryService].to[UserHistoryServiceImpl]
    bind[UserLeftGuildDAO].to[UserLeftGuildDAOImpl]

    bind[TournamentSeriesService].to[TournamentSeriesServiceImpl]
    bind[ReplayActionBuilderService].to[ReplayActionBuilderServiceImpl]
  }
}
