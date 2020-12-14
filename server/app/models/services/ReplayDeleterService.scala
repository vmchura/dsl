package models.services

import jobs.JobError

import java.util.UUID
import scala.concurrent.Future

trait ReplayDeleterService {
  def disableReplay(replayID: UUID): Future[Either[JobError, Boolean]]
}
