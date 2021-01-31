package modules.teamsystem

import com.google.inject.AbstractModule
import models.daos.{DiscordPlayerLoggedDAO, DiscordPlayerLoggedDAOImpl}
import models.daos.teamsystem.{
  InvitationDAO,
  InvitationDAOImpl,
  RequestDAO,
  RequestDAOImpl,
  TeamDAO,
  TeamDAOImpl
}
import net.codingwell.scalaguice.ScalaModule
import play.api.libs.concurrent.AkkaGuiceSupport

class TeamSystemModule
    extends AbstractModule
    with ScalaModule
    with AkkaGuiceSupport {
  override def configure(): Unit = {
    bind[TeamDAO].to[TeamDAOImpl]
    bind[InvitationDAO].to[InvitationDAOImpl]
    bind[RequestDAO].to[RequestDAOImpl]
    bind[DiscordPlayerLoggedDAO].to[DiscordPlayerLoggedDAOImpl]
    bindTypedActor(
      DiscordPlayerSupervisor,
      "Discord-player-supervisor-actor"
    )
    bindTypedActor(
      TeamCreator,
      "Team-creator-manager-actor"
    )
    bindTypedActor(
      InvitationManager,
      "Team-invitation-manager-actor"
    )
    bindTypedActor(TeamManager, "Team-manager-actor")
    bindTypedActor(RequestJoinManager, "Team-request-manager-actor")
  }
}
