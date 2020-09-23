package models.daos

import java.util.UUID

import org.joda.time.DateTime

trait TicketReplayDAO {
  def ableToUpload(userID: UUID, now: DateTime): Boolean
  def uploading(userID: UUID, now: DateTime): Unit
  def ableToDownload(userID: UUID, now: DateTime): Boolean
  def downloading(userID: UUID, now: DateTime): Unit
}
