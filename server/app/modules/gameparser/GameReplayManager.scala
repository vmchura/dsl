package modules.gameparser

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import com.google.inject.Provides
import modules.gameparser.GameJudge.JudgeGame
import modules.gameparser.GameParser._
import play.api.libs.concurrent.ActorModule
import models.StarCraftModels._
import models.services.ParseReplayFileService

import java.io.File

object GameReplayManager extends ActorModule {
  override type Message = ManagerCommand
  trait ManagerCommand
  case class ManageGameReplay(file: File, replyTo: ActorRef[SCGameMode])
      extends ManagerCommand
  case class ManageGameInfo(gameInfo: GameInfo, replyTo: ActorRef[SCGameMode])
      extends ManagerCommand
  @Provides
  def create(
      parseReplayFileService: ParseReplayFileService
  ): Behavior[ManagerCommand] = {
    Behaviors.setup { context =>
      def backendResponseMapper(
          replyTo: ActorRef[SCGameMode]
      ): ActorRef[GameInfo] =
        context.messageAdapter(rsp => ManageGameInfo(rsp, replyTo))

      Behaviors.receiveMessage[ManagerCommand] { message =>
        message match {
          case ManageGameReplay(file, replyTo) =>
            val parser =
              context.spawnAnonymous(GameParser(parseReplayFileService))
            parser ! ReplayToParse(file, backendResponseMapper(replyTo))
          case ManageGameInfo(gameInfo, replyTo) =>
            gameInfo match {
              case rep: ReplayParsed =>
                val judger = context.spawnAnonymous(GameJudge())
                judger ! JudgeGame(rep, replyTo)
              case ImpossibleToParse =>
                replyTo ! InvalidSCGameMode(Nil)
            }
        }
        Behaviors.same

      }
    }
  }

}
