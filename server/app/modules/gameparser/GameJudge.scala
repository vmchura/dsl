package modules.gameparser

import akka.actor.typed.scaladsl.Behaviors
import modules.gameparser.GameParser.{ReplayParsed, Team}
import play.api.libs.concurrent.ActorModule
import akka.actor.typed.ActorRef
import com.google.inject.Provides
import shared.models.StarCraftModels._
object GameJudge extends ActorModule {
  override type Message = JudgeGame
  case class JudgeGame(gameInfo: ReplayParsed, replyTo: ActorRef[SCGameMode])
  @Provides
  def apply(): Behaviors.Receive[JudgeGame] =
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
                  case (winner :: Nil, loser :: Nil) => OneVsOne(winner, loser)
                  case _                             => InvalidSCGameMode(winners ::: losers)
                }
                replayParsed.gameMode match {
                  case OneVsOneMode | TopVsBottom | Melee | DangerMode =>
                    resultOneVsOne
                  case _ => defaultOnError
                }
              } else {
                replayParsed match {
                  case ReplayParsed(TopVsBottom, _, _) =>
                    ManyVsMany(winners, losers)
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
