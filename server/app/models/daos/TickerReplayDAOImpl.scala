package models.daos
import java.util.UUID
import org.joda.time.DateTime
import play.api.Configuration

import javax.inject.Inject

class TickerReplayDAOImpl @Inject() (configuration: Configuration)
    extends TicketReplayDAO {
  val upload =
    new TicketManager(
      configuration.get[Int]("user-ticket.time-window.upload"),
      1
    )
  val download =
    new TicketManager(
      configuration.get[Int]("user-ticket.time-window.download"),
      30
    )
  override def ableToUpload(userID: UUID, now: DateTime): Boolean =
    upload.isAble(userID, now)

  override def uploading(userID: UUID, now: DateTime): Unit =
    upload.marksAsUsing(userID, now)

  override def ableToDownload(userID: UUID, now: DateTime): Boolean =
    download.isAble(userID, now)

  override def downloading(userID: UUID, now: DateTime): Unit =
    download.marksAsUsing(userID, now)
}
