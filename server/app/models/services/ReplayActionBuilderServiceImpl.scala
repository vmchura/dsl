package models.services

import jobs.FileIsAlreadyRegistered
import models.UserSmurf
import models.daos.{ReplayMatchDAO, UserSmurfDAO}
import modules.gameparser.GameReplayManager.ManageGameReplay

import scala.concurrent.ExecutionContext.Implicits.global
import akka.actor.typed.ActorRef
import modules.gameparser.GameReplayManager.ManagerCommand
import akka.actor.typed.scaladsl.AskPattern._
import akka.util.Timeout

import scala.concurrent.duration._
import java.io.File
import javax.inject.Inject
import scala.concurrent.Future
import jobs._
import shared.models.StarCraftModels.{OneVsOne, SCPlayer}
import shared.models.{
  ChallongeOneVsOneMatchGameResult,
  ChallongePlayer,
  DiscordIDSource
}
class ReplayActionBuilderServiceImpl @Inject() (
    override val parseReplayService: ParseReplayFileService,
    userSmurfDAO: UserSmurfDAO,
    replayMatchDAO: ReplayMatchDAO,
    replayGameManager: ActorRef[ManagerCommand]
)(implicit scheduler: akka.actor.typed.Scheduler)
    extends ReplayActionBuilderService {
  implicit val timeout: Timeout = 5.seconds

  override def parseFileAndBuildAction(
      file: File,
      discordUserID1: String,
      discordUserID2: String
  ): Future[Either[String, ChallongeOneVsOneMatchGameResult]] = {

    case class DiscordUserRegistered(
        noDiscordUserFound: Boolean,
        foundOnlyOne: Boolean,
        anyDiscordUserIfMoreFound: Option[UserSmurf],
        discordUserIfOnlyOneFound: Option[UserSmurf]
    )

    val result = {
      val message = for {
        fileIsUnique <- replayMatchDAO.isNotRegistered(file)
        _ <- fileIsUnique.withFailure(FileIsAlreadyRegistered)
        parsedFromActor <-
          replayGameManager.ask(ref => ManageGameReplay(file, ref))
        oneVsOneGame <- parsedFromActor match {
          case oneVsOne: OneVsOne => Future.successful(oneVsOne)
          case _ =>
            Future.failed(new IllegalArgumentException("Replay is not 1v1"))
        }
        participantsWithWinnerSmurf <-
          userSmurfDAO.findBySmurf(oneVsOneGame.winner.smurf)
        participantsWithLoserSmurf <-
          userSmurfDAO.findBySmurf(oneVsOneGame.loser.smurf)
      } yield {
        val buildChallongePlayer
            : List[UserSmurf] => SCPlayer => ChallongePlayer = { candidates =>
          val discordID = candidates match {
            case Nil => Right(DiscordIDSource.buildByHistory())
            case UserSmurf(discordUser, _, _) :: Nil
                if discordUser.discordID.equals(
                  discordUserID1
                ) || discordUser.discordID.equals(discordUserID2) =>
              Right(DiscordIDSource.buildByHistory(discordUser.discordID))
            case _ =>
              Left("Too many possibilities")
          }
          pl => ChallongePlayer(discordID, pl)
        }
        val challongeWinner =
          buildChallongePlayer(participantsWithWinnerSmurf)(oneVsOneGame.winner)
        val challongeLoser =
          buildChallongePlayer(participantsWithLoserSmurf)(oneVsOneGame.loser)

        if (
          oneVsOneGame.winner.smurf.equals(
            oneVsOneGame.loser.smurf
          ) || challongeWinner.discordID.equals(challongeLoser.discordID)
        ) {
          ChallongeOneVsOneMatchGameResult(
            ChallongePlayer(
              Left("Impossible, same smurfs"),
              oneVsOneGame.winner
            ),
            ChallongePlayer(Left("Impossible, same smurfs"), oneVsOneGame.loser)
          )
        } else {

          def theOtherID(discordID: String): String =
            if (discordID.equals(discordUserID1)) discordUserID2
            else discordUserID1

          (challongeWinner, challongeLoser) match {

            case (
                  ChallongePlayer(Right(DiscordIDSource(Right(None))), _),
                  ChallongePlayer(
                    Right(DiscordIDSource(Right(Some(discordIDLoser)))),
                    _
                  )
                ) =>
              ChallongeOneVsOneMatchGameResult(
                challongeWinner
                  .copy(discordID =
                    Right(
                      DiscordIDSource.buildByLogic(theOtherID(discordIDLoser))
                    )
                  ),
                challongeLoser
              )
            case (
                  ChallongePlayer(
                    Right(DiscordIDSource(Right(Some(discordIDWinner)))),
                    _
                  ),
                  ChallongePlayer(Right(DiscordIDSource(Right(None))), _)
                ) =>
              ChallongeOneVsOneMatchGameResult(
                challongeWinner,
                challongeLoser
                  .copy(discordID =
                    Right(
                      DiscordIDSource.buildByLogic(theOtherID(discordIDWinner))
                    )
                  )
              )
            case _ =>
              ChallongeOneVsOneMatchGameResult(challongeWinner, challongeLoser)

          }

        }

      }

      message.map(v => Right(v)).recover(e => Left(e.toString))

    }
    result

  }
}
