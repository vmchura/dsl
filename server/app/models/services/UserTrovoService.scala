package models.services
import models.TrovoUser

import scala.concurrent.Future
trait UserTrovoService {
  def all(): Future[Seq[TrovoUser]]
  def save(trovoUser: TrovoUser): Future[Option[TrovoUser]]
}
