package models.services
import scala.concurrent.ExecutionContext.Implicits.global
import javax.inject.Inject
import models.{DiscordUserData, GuildID, UserGuild, UserLeftGuild}
import models.daos.{UserGuildDAO, UserHistoryDAO, UserLeftGuildDAO}

import scala.concurrent.Future
class UserHistoryServiceImpl @Inject() (
    userHistory: UserHistoryDAO,
    userGuild: UserGuildDAO,
    discordUserService: DiscordUserService,
    userLeftGuildDAO: UserLeftGuildDAO
) extends UserHistoryService {
  def traverse[A, R](a: List[A])(f: A => Future[R]): Future[List[R]] = {
    a.foldLeft(Future.successful(List.empty[R])) {
      case (listaFut, a) =>
        for {
          lista <- listaFut
          r <- f(a)
        } yield {
          r :: lista
        }
    }
  }
  override def update(): Future[Int] = {
    for {
      guildUser <- userGuild.all()

      historyUsers <- userHistory.all()

      notHistoriedGuildUsers <- Future.successful(
        guildUser.filterNot(gu =>
          historyUsers.exists(_.discordID == gu.discordID)
        )
      )

      notHistoriedButNotMarked <- traverse(
        notHistoriedGuildUsers
          .flatMap(ug => ug.guilds.toList.map(g => (ug.discordID, g)))
          .toList
      ) {
        case (userDiscordID, guildID) =>
          userLeftGuildDAO
            .userIsGone(UserLeftGuild(userDiscordID, guildID))
            .map { isGone =>
              if (!isGone) Some((userDiscordID, guildID)) else None
            }
      }

      requestGuildData <- traverse(notHistoriedButNotMarked.flatten) {
        case (userDiscordID, guildID) =>
          discordUserService
            .findMemberOnGuildData(guildID, userDiscordID)
            .map { _.map(gd => (gd, guildID)) }
      }

      singleUpdates <- {
        traverse(
          requestGuildData
        ) { rg =>
          rg.fold(Future.successful(0)) {
            case (gd, g) =>
              update(gd, g)
          }
        }
      }

    } yield {

      singleUpdates.sum
    }
  }

  override def update(
      discordUserData: DiscordUserData,
      guildID: models.GuildID
  ): Future[Int] = {
    for {
      update <-
        userHistory
          .updateWithLastInformation(
            discordUserData.discordID,
            guildID,
            discordUserData
          )
          .map(inserted => if (inserted) 1 else 0)

    } yield {
      update
    }

  }
}
