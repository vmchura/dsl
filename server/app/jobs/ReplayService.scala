package jobs
import java.io.File
import java.util.UUID
import javax.inject.Inject
import models.{ReplayRecord, User}
import models.daos.ReplayMatchDAO
import models.services.{
  ChallongeTournamentService,
  DiscordFileService,
  DropBoxFilesService,
  ParseReplayFileService,
  S3FilesService,
  TournamentService
}
import modules.gameparser.GameParser.{GameInfo, ReplayParsed}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ReplayService @Inject() (
    tournamentService: TournamentService,
    challongeTournamentService: ChallongeTournamentService,
    dropBoxFilesService: DropBoxFilesService,
    replayMatchDAO: ReplayMatchDAO,
    s3FilesService: S3FilesService,
    discordFileService: DiscordFileService,
    parseReplayFileService: ParseReplayFileService
) {

  def formFuture[A](f: Future[A]): Future[Either[JobError, A]] =
    convertToEither(UnknowReplayPusherError)(f)

  def pushReplay(
      tournamentID: Long,
      matchID: Long,
      replay: File,
      user: User,
      fileName: String
  )(newIDForThisReplay: UUID): Future[Either[JobError, Boolean]] = {
    val executionFuture = for {
      tournamentOpt <- tournamentService.loadTournament(tournamentID)
      tournament <-
        tournamentOpt.withFailure(TournamentNotFoundToReplay(tournamentID))
      challongeTournamentOpt <-
        challongeTournamentService.findChallongeTournament(
          tournament.discordServerID
        )(tournament.urlID)
      challongeTournament <- challongeTournamentOpt.withFailure(
        TournamentNotFoundOnChallonge(tournament.urlID)
      )
      matchChallonge <-
        challongeTournament.matches
          .find(_.matchPK.challongeMatchID == matchID)
          .withFailure(MatchNotFoundOnChallonge(matchID))
      insertionOnDropBox <- dropBoxFilesService.push(
        replay,
        matchChallonge.asMatchName(newIDForThisReplay)
      )
      _ <- insertionOnDropBox.withFailure(CannotInsertOnDropBox)
      s3Insertion <- s3FilesService.push(
        replay,
        matchChallonge.asMatchName(newIDForThisReplay)
      )
      _ <- s3Insertion.withFailure(CannotInsertS3)
      discordInsertion <-
        tournament.channelDiscordReplay.fold(Future.successful(true))(ch =>
          discordFileService.pushFileOnChannel(
            ch,
            replay,
            "",
            matchChallonge.asMatchName(newIDForThisReplay).pathFileOnCloud
          )
        )
      gameInfo <- parseReplayFileService.parseFile(replay).map(GameInfo.apply)
      _ <- discordInsertion.withFailure(CannotInsertDiscord)
      insertionOnDB <- replayMatchDAO.add(
        ReplayRecord(
          newIDForThisReplay,
          ReplayRecord.md5HashString(replay),
          matchChallonge.asMatchName(newIDForThisReplay).pathFileOnCloud,
          fileName,
          tournamentID,
          matchID,
          enabled = true,
          user.loginInfo.providerKey, {
            gameInfo match {
              case ReplayParsed(_, startTime, _, _, _) => startTime
              case _                                   => None
            }
          }
        )
      )
    } yield {
      insertionOnDB
    }
    formFuture(executionFuture)
  }
  def downloadReplay(
      replayID: UUID,
      replayName: String
  ): Future[Either[JobError, File]] = {
    val f = for {
      file <- dropBoxFilesService.download(replayID, replayName)
    } yield {
      file
    }
    formFuture(f)
  }

  def wrapIntoFolder(
      replay: ReplayRecord,
      folderName: String
  ): Future[Option[String]] = {
    for {
      moveOption <-
        dropBoxFilesService.wrapIntoFolder(replay.matchName, folderName)

    } yield {
      moveOption
    }
  }

  private def createFolders(
      anyRep: ReplayRecord,
      bof: Int
  ): Future[Seq[String]] = {

    val parentFolder =
      anyRep.matchName.substring(0, anyRep.matchName.lastIndexOf('/'))
    val folders = (1 to bof).map(i => s"Game$i")
    dropBoxFilesService.createFoldersAt(parentFolder, folders).map { created =>
      if (created) folders
      else Nil
    }

  }
  def createFoldersAntiSpoilers(
      tournamentID: Long,
      matchID: Long,
      bof: Int,
      order: Seq[UUID]
  ): Future[Either[JobError, Boolean]] = {
    val executionFuture = for {
      replays <- replayMatchDAO.loadAllByMatch(tournamentID, matchID)
      _ <- (order.sorted == replays.filter(_.enabled).map(_.replayID).sorted)
        .withFailure(NotCompleteMatches)
      anyrep <- replays.find(_.enabled).withFailure(NoReplaysEnabled)
      folders <- createFolders(anyrep, bof)
      _ <- (bof >= order.length).withFailure(TooManyGames)
      wrapped <- Future.sequence(order.zipWithIndex.map {
        case (id, indx) =>
          for {
            replay <-
              replays.find(_.replayID == id).withFailure(BadOrderReplays)
            pathResultOpt <- wrapIntoFolder(replay, folders(indx))
            pathResult <- pathResultOpt.withFailure(CannotWrapIntoFolder)
            locationUpdated <- replayMatchDAO.updateLocation(id, pathResult)
          } yield {
            locationUpdated
          }
      })
    } yield {

      wrapped.forall(i => i)
    }
    formFuture(executionFuture)

  }

}
