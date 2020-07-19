package jobs
import java.io.File

import javax.inject.Inject
import models.services.{ChallongeTournamentService, DropBoxFilesService, TournamentService}
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future

class ReplayPusher  @Inject() (tournamentService: TournamentService,
                               challongeTournamentService: ChallongeTournamentService,
                               dropBoxFilesService: DropBoxFilesService) {
  def formFuture[A](f: Future[A]): Future[Either[JobError,A]] = {
    convertToEither(s => UnknowReplayPusherError(s))(f)

  }

  def pushReplay(tournamentID: Long, matchID: Long, replay: File): Future[Either[JobError,Boolean]] = {
    val executionFuture = for{
      tournamentOpt <- tournamentService.loadTournament(tournamentID)
      tournament <- tournamentOpt.withFailure(TournamentNotFoundToReplay(tournamentID))
      challongeTournamentOpt <- challongeTournamentService.findChallongeTournament(tournament.discordServerID)(tournament.urlID)
      challongeTournament <- challongeTournamentOpt.withFailure(TournamentNotFoundOnChallonge(tournament.urlID))
      matchChallonge <- challongeTournament.matches.find(_.matchPK.challongeMatchID == matchID).withFailure(MatchNotFoundOnChallonge(matchID))
      insertionOnDropBox <- dropBoxFilesService.push(replay,matchChallonge.asMatchName())
      insertion <- insertionOnDropBox.withFailure(CannotInsertOnDropBox)
    }yield{
      insertion
    }
    formFuture(executionFuture)
  }

}
