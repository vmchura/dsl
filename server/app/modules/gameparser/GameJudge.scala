package modules.gameparser

import akka.actor.typed.scaladsl.Behaviors
import modules.gameparser.GameParser.{ReplayParsed, Team}
import akka.actor.typed.{ActorRef, Behavior}
import shared.models.StarCraftModels._
object GameJudge {
  case class JudgeGame(gameInfo: ReplayParsed, replyTo: ActorRef[SCGameMode])
  def apply(): Behavior[JudgeGame] =
    Behaviors.receive { (_, command) =>
      command match {
        case JudgeGame(replayParsed, replyTo) =>
          val defaultOnError =
            InvalidSCGameMode(replayParsed.teams.flatMap(_.participants))
          val is1v1: Boolean =
            replayParsed.teams.forall(_.participants.length == 1)
          val winnerDefined: Boolean =
            replayParsed.teams.exists(
              _.index == replayParsed.winnerTeamIndex
            ) &&
              replayParsed.teams.length == 2
          val (winners, losers) = if (winnerDefined) {
            replayParsed.teams.partition(
              _.index == replayParsed.winnerTeamIndex
            ) match {
              case (Team(_, team0) :: _, Team(_, team1) :: _) => (team0, team1)
              case (_, _)                                     => (Nil, Nil)
            }
          } else {
            (Nil, Nil)
          }
          val gameInfo: SCGameMode = {
            if (winnerDefined) {
              if (is1v1) {
                val resultOneVsOne = (winners, losers) match {
                  case (winner :: Nil, loser :: Nil) =>
                    OneVsOne(
                      winner,
                      loser,
                      replayParsed.mapName.getOrElse("???"),
                      replayParsed.startTime.getOrElse("???")
                    )
                  case _ => InvalidSCGameMode(winners ::: losers)
                }
                replayParsed.gameMode match {
                  case OneVsOneMode | TopVsBottom | Melee | DangerMode =>
                    resultOneVsOne
                  case _ => defaultOnError
                }
              } else {
                replayParsed match {
                  case ReplayParsed(_, _, TopVsBottom, _, _) =>
                    ManyVsMany(
                      winners,
                      losers,
                      replayParsed.mapName.getOrElse("???"),
                      replayParsed.startTime.getOrElse("???")
                    )
                  case _ => defaultOnError
                }
              }
            } else {
              defaultOnError
            }

          }
          replyTo ! gameInfo
          Behaviors.stopped
      }
    }

}
