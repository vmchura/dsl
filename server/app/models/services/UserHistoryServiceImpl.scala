package models.services
import scala.concurrent.ExecutionContext.Implicits.global
import javax.inject.Inject
import models.{DiscordUserData, GuildID}
import models.daos.{UserGuildDAO, UserHistoryDAO}

import scala.concurrent.Future
class UserHistoryServiceImpl @Inject()(userHistory: UserHistoryDAO,
                                       userGuild: UserGuildDAO,
                                       discordUserService: DiscordUserService) extends UserHistoryService {
  override def update(): Future[Int] = {
    for{
      allUsers <- userGuild.all()
      disctinctGuilds <-  Future.sequence(allUsers.flatMap(_.guilds).distinct.map( g =>  discordUserService.findMembersOnGuildData(g).map(r => g -> r)))
      mapGuilds <- Future.successful(disctinctGuilds.flatMap{case (g,result) => result.map(r => g -> r)}.toMap)
      singleUpdates <- Future.sequence(allUsers.flatMap(u => u.guilds.map(g => update(u.discordID,g,mapGuilds))))
    }yield{

      singleUpdates.sum
    }
  }

  override def update(discordID: models.DiscordID, guildID: models.GuildID, guilds: Map[GuildID,Seq[DiscordUserData]]): Future[Int] = {
    for{
      userOpt <- Future.successful(guilds.get(guildID).flatMap(_.find(_.discordID==discordID)))
      update <- userOpt match {
        case Some(u) =>  userHistory.updateWithLastInformation(discordID,guildID, u).map(inserted => if(inserted) 1 else 0)
        case None => Future.successful(0)
      }
    }yield{
      update
    }

  }
}
