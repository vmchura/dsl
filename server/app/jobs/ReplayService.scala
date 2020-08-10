package jobs
import java.io.File
import java.util.UUID

import javax.inject.Inject
import models.{ReplayRecord, User}
import models.daos.ReplayMatchDAO
import models.services.{ChallongeTournamentService, DropBoxFilesService, TournamentService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ReplayService  @Inject()(tournamentService: TournamentService,
                               challongeTournamentService: ChallongeTournamentService,
                               dropBoxFilesService: DropBoxFilesService,
                               replayMatchDAO: ReplayMatchDAO) {
  def formFuture[A](f: Future[A]): Future[Either[JobError,A]] = {
    convertToEither(s => UnknowReplayPusherError(s))(f)

  }

  def pushReplay(tournamentID: Long, matchID: Long, replay: File, user: User, fileName: String): Future[Either[JobError,Boolean]] = {
    val newIDForThisReplay = UUID.randomUUID()
    val executionFuture = for{
      tournamentOpt <- tournamentService.loadTournament(tournamentID)
      tournament <- tournamentOpt.withFailure(TournamentNotFoundToReplay(tournamentID))
      challongeTournamentOpt <- challongeTournamentService.findChallongeTournament(tournament.discordServerID)(tournament.urlID)
      challongeTournament <- challongeTournamentOpt.withFailure(TournamentNotFoundOnChallonge(tournament.urlID))
      matchChallonge <- challongeTournament.matches.find(_.matchPK.challongeMatchID == matchID).withFailure(MatchNotFoundOnChallonge(matchID))
      insertionOnDropBox <- dropBoxFilesService.push(replay,matchChallonge.asMatchName(newIDForThisReplay))
      _ <- insertionOnDropBox.withFailure(CannotInsertOnDropBox)
      insertionOnDB <- replayMatchDAO.add(ReplayRecord(newIDForThisReplay,
        matchChallonge.asMatchName(newIDForThisReplay).pathFileOnCloud,
        fileName,tournamentID,matchID,enabled = true,user.loginInfo.providerKey))
    }yield{
      insertionOnDB
    }
    formFuture(executionFuture)
  }
  def downloadReplay(replayID: UUID, replayName: String): Future[Either[JobError,File]] = {
    val f = for{
      file <- dropBoxFilesService.download(replayID, replayName)
    }yield{
      file
    }
    formFuture(f)
  }



}
