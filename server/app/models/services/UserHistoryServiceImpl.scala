package models.services
import scala.concurrent.ExecutionContext.Implicits.global
import javax.inject.Inject
import models.DiscordUserData
import models.daos.{UserGuildDAO, UserHistoryDAO}

import scala.concurrent.Future
class UserHistoryServiceImpl @Inject() (
    userHistory: UserHistoryDAO,
    userGuild: UserGuildDAO,
    discordUserService: DiscordUserService
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

      guilds <- userGuild.guilds()
      guildUser <-
        Future
          .traverse(guilds.toList)(g =>
            discordUserService
              .findMembersOnGuildData(g)
              .map(r => r.map(x => g -> x))
          )
          .map(_.flatten)
      singleUpdates <- {
        traverse(
          guildUser.flatMap { case (g, users) => users.map(u => g -> u) }
        ) { case (g, u) => update(u, g) }
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
