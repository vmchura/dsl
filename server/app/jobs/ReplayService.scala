package jobs
import java.io.File
import java.util.UUID

import javax.inject.Inject
import models.{ReplayRecord, User}
import models.daos.ReplayMatchDAO
import models.services.{ChallongeTournamentService, DiscordFileService, DropBoxFilesService, ParseReplayFileService, S3FilesService, TournamentService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ReplayService  @Inject()(tournamentService: TournamentService,
                               challongeTournamentService: ChallongeTournamentService,
                               dropBoxFilesService: DropBoxFilesService,
                               replayMatchDAO: ReplayMatchDAO,
                               s3FilesService: S3FilesService,
                               discordFileService: DiscordFileService,
                               parseReplayFiseService: ParseReplayFileService) {

  def formFuture[A](f: Future[A]): Future[Either[JobError,A]] = convertToEither(UnknowReplayPusherError)(f)

  def pushReplay(tournamentID: Long, matchID: Long, replay: File, user: User, fileName: String)(newIDForThisReplay: UUID): Future[Either[JobError,Boolean]] = {
    val executionFuture = for{
      tournamentOpt <- tournamentService.loadTournament(tournamentID)
      tournament <- tournamentOpt.withFailure(TournamentNotFoundToReplay(tournamentID))
      challongeTournamentOpt <- challongeTournamentService.findChallongeTournament(tournament.discordServerID)(tournament.urlID)
      challongeTournament <- challongeTournamentOpt.withFailure(TournamentNotFoundOnChallonge(tournament.urlID))
      matchChallonge <- challongeTournament.matches.find(_.matchPK.challongeMatchID == matchID).withFailure(MatchNotFoundOnChallonge(matchID))
      parsedEither <- parseReplayFiseService.parseFileAndBuildDescription(replay)
      parsed <- parsedEither.withFailure
      insertionOnDropBox <- dropBoxFilesService.push(replay,matchChallonge.asMatchName(newIDForThisReplay))
      _ <- insertionOnDropBox.withFailure(CannotInsertOnDropBox)
      s3Insertion <- s3FilesService.push(replay,matchChallonge.asMatchName(newIDForThisReplay))
      _ <- s3Insertion.withFailure(CannotInsertS3)
      discordInsertion <- tournament.channelDiscordReplay.fold(Future.successful(true))(ch => discordFileService.pushFileOnChannel(ch,replay,"",matchChallonge.asMatchName(newIDForThisReplay).pathFileOnCloud))
      _ <- discordInsertion.withFailure(CannotInsertDiscord)
      insertionOnDB <- replayMatchDAO.add(ReplayRecord(newIDForThisReplay,ReplayRecord.md5HashString(replay),
        matchChallonge.asMatchName(newIDForThisReplay).pathFileOnCloud,
        fileName,tournamentID,matchID,enabled = true,user.loginInfo.providerKey,parsed.dateGame))
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

  def disableReplay(replayID: UUID): Future[Either[JobError,Boolean]] = {
    val executionFuture = for{
      replayOpt <- replayMatchDAO.find(replayID)
      deleteSpicific <- replayOpt.fold(Future.successful(true))(replay => dropBoxFilesService.delete(replay.matchName))
      _ <- deleteSpicific.withFailure(TournamentNotFoundToReplay(0L))
      insertionOnDB <- replayOpt.fold(Future.successful(true))(replay => replayMatchDAO.markAsDisabled(replay.replayID))
    }yield{
      insertionOnDB
    }
    formFuture(executionFuture)
  }



}
