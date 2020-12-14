package models.services
import jobs._
import models.daos.ReplayMatchDAO

import java.util.UUID
import scala.concurrent.Future
import javax.inject.Inject
import scala.concurrent.ExecutionContext.Implicits.global

class ReplayDeleterServiceImpl @Inject() (
    dropBoxFilesService: DropBoxFilesService,
    replayMatchDAO: ReplayMatchDAO
) extends ReplayDeleterService {
  def formFuture[A](f: Future[A]): Future[Either[JobError, A]] =
    convertToEither(UnknowReplayPusherError)(f)

  override def disableReplay(
      replayID: UUID
  ): Future[Either[jobs.JobError, Boolean]] = {
    val executionFuture = for {
      replayOpt <- replayMatchDAO.find(replayID)
      deleteSpicific <- replayOpt.fold(Future.successful(true))(replay =>
        dropBoxFilesService.delete(replay.matchName)
      )
      _ <- deleteSpicific.withFailure(TournamentNotFoundToReplay(0L))
      insertionOnDB <- replayOpt.fold(Future.successful(true))(replay =>
        replayMatchDAO.markAsDisabled(replay.replayID)
      )
    } yield {
      insertionOnDB
    }
    formFuture(executionFuture)
  }
}
