package jobs
import java.io.File
import java.util.UUID

import javax.inject.Inject
import models.{ReplayRecord, User}
import models.daos.ReplayMatchDAO
import models.services.{ChallongeTournamentService, DropBoxFilesService, TournamentService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ReplayPusher  @Inject() (tournamentService: TournamentService,
                               challongeTournamentService: ChallongeTournamentService,
                               dropBoxFilesService: DropBoxFilesService,
                               replayMatchDAO: ReplayMatchDAO) {
  def formFuture[A](f: Future[A]): Future[Either[JobError,A]] = {
    convertToEither(s => UnknowReplayPusherError(s))(f)

  }

  def pushReplay(tournamentID: Long, matchID: Long, replay: File, user: User, fileName: String): Future[Either[JobError,Boolean]] = {
    val executionFuture = for{
      tournamentOpt <- tournamentService.loadTournament(tournamentID)
      tournament <- tournamentOpt.withFailure(TournamentNotFoundToReplay(tournamentID))
      challongeTournamentOpt <- challongeTournamentService.findChallongeTournament(tournament.discordServerID)(tournament.urlID)
      challongeTournament <- challongeTournamentOpt.withFailure(TournamentNotFoundOnChallonge(tournament.urlID))
      matchChallonge <- challongeTournament.matches.find(_.matchPK.challongeMatchID == matchID).withFailure(MatchNotFoundOnChallonge(matchID))
      insertionOnDropBox <- dropBoxFilesService.push(replay,matchChallonge.asMatchName())
      _ <- insertionOnDropBox.withFailure(CannotInsertOnDropBox)
      insertionOnDB <- replayMatchDAO.add(ReplayRecord(UUID.randomUUID(),
        matchChallonge.asMatchName().toString,
        fileName,tournamentID,matchID,enabled = true,user.loginInfo.providerKey))
    }yield{
      insertionOnDB
    }
    formFuture(executionFuture)
  }


}
