package models.services
import com.google.inject.Inject
import models.TrovoUser
import models.daos.TrovoUserDAO

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
class UserTrovoServiceImpl @Inject() (trovoUserDAO: TrovoUserDAO)
    extends UserTrovoService {
  override def all(): Future[Seq[TrovoUser]] = trovoUserDAO.all()

  override def save(trovoUser: TrovoUser): Future[Option[TrovoUser]] =
    for {
      currentUserHoldingTrovoID <- trovoUserDAO.find(trovoUser.trovoUserID)
      deletion <- currentUserHoldingTrovoID.fold(Future.successful(true)) {
        ou =>
          if (!ou.discordID.equals(trovoUser.discordID)) {
            trovoUserDAO.remove(ou.discordID)
          } else {
            Future.successful(true)
          }
      }
      _ <-
        if (deletion) Future.successful(())
        else throw new IllegalStateException("2 users holding same trovo ID")
      currentDiscordUser <- trovoUserDAO.find(trovoUser.discordID)
      insertion <- currentDiscordUser.fold(trovoUserDAO.save(trovoUser)) { _ =>
        trovoUserDAO.update(trovoUser)
      }
    } yield {
      insertion
    }
}
