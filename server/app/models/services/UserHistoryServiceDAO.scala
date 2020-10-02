package models.services
import scala.concurrent.ExecutionContext.Implicits.global
import javax.inject.Inject
import models.daos.{UserGuildDAO, UserHistoryDAO}

import scala.concurrent.Future
class UserHistoryServiceDAO @Inject() (userHistory: UserHistoryDAO,
                                       userGuild: UserGuildDAO,
                                      discordUserService: DiscordUserService) extends UserHistoryService {
  override def update(): Future[Int] = {
    for{
      allUsers <- userGuild.all()
      singleUpdates <- Future.sequence(allUsers.flatMap(u => u.guilds.map(g => update(u.discordID,g))))
    }yield{
      singleUpdates.sum
    }
  }

  override def update(discordID: models.DiscordID, guildID: models.GuildID): Future[Int] = {
    for{
      userOpt <- discordUserService.loadSingleUser(guildID,discordID)
      update <- userOpt match {
        case Some(u) => userHistory.updateWithLastInformation(discordID,guildID, u).map(inserted => if(inserted) 1 else 0)
        case None => Future.successful(0)
      }
    }yield{
      update
    }

  }
}
