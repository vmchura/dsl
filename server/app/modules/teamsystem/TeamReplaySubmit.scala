package modules.teamsystem

import akka.actor.typed.{ActorRef, Behavior, scaladsl}
import akka.actor.typed.scaladsl.Behaviors
import models.services.ParseReplayFileService
import models.{ReplayRecord, Smurf}
import models.teamsystem.TeamID
import modules.gameparser.GameJudge
import modules.gameparser.GameParser.{GameInfo, ImpossibleToParse, ReplayParsed}
import shared.models.StarCraftModels.{OneVsOne, OneVsOneMode}
import shared.models.{DiscordID, DiscordPlayerLogged, ReplayTeamID}

import java.io.File
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global
object TeamReplaySubmit {

  sealed trait InternalCommand
  sealed trait Command extends InternalCommand
  case class Submit(
      id: ReplayTeamID,
      senderID: DiscordID,
      teamID: TeamID,
      replay: File,
      replyTo: ActorRef[Response],
      parent: ActorRef[TeamReplayManager.Command]
  ) extends Command
  case class SmurfSelected(
      smurf: Smurf,
      replyTo: ActorRef[Response]
  ) extends Command

  sealed trait Response
  case class SubmitError(reason: String) extends Response
  case class ReplaySavedResponse() extends Response
  case class SmurfMustBeSelectedResponse(oneVsOne: OneVsOne) extends Response

  case class Unique() extends InternalCommand
  case class Duplicated() extends InternalCommand
  case class Pending() extends InternalCommand
  case class BothSmurfsFree(oneVsOne: OneVsOne) extends InternalCommand
  case class WinnerOwnerResponse(response: UniqueSmurfWatcher.Response)
      extends InternalCommand
  case class GameParsed(oneVsOne: OneVsOne) extends InternalCommand
  case class LoserOwnerResponse(response: UniqueSmurfWatcher.Response)
      extends InternalCommand
  case class ReplayParsedMessage(replayParsed: ReplayParsed)
      extends InternalCommand
  case class ReplayErrorParsing(reason: String) extends InternalCommand
  case class SmurfSenderValid(oneVsOne: OneVsOne, smurf: Smurf)
      extends InternalCommand

  case class SmurfSentToLeader() extends InternalCommand
  case class ReplayInfoSaved() extends InternalCommand
  case class ReplaySaved() extends InternalCommand

  def replaySaving(implicit
      replyTo: ActorRef[Response]
  ): Behavior[InternalCommand] =
    Behaviors.receiveMessage {
      case ReplaySaved() =>
        replyTo ! ReplaySavedResponse()
        Behaviors.stopped
      case _ =>
        replyTo ! SubmitError("Replay no se ha guardado satisfactoriamente")
        Behaviors.stopped
    }

  def replayInfoSaving(
      metaInfoReplay: MetaInfoReplay
  )(implicit
      replyTo: ActorRef[Response]
  ): Behavior[InternalCommand] =
    Behaviors.receiveMessage {
      case ReplayInfoSaved() =>
        //TODO push and save replay file
        replaySaving
      case _ =>
        replyTo ! SubmitError(
          "Replay Info no se ha guardado satisfactoriamente"
        )
        Behaviors.stopped
    }

  def smurfRelationSaving(
      metaInfoReplay: MetaInfoReplay
  )(implicit
      replyTo: ActorRef[Response]
  ): Behavior[InternalCommand] =
    Behaviors.receiveMessage {
      case SmurfSentToLeader() =>
        //TODO push info
        replayInfoSaving(metaInfoReplay)
      case _ =>
        replyTo ! SubmitError(
          "El smurf no ha podido enviarse al lider del equipo"
        )
        Behaviors.stopped
    }

  def awaitSmurfOfSender(
      metaInfoReplay: MetaInfoReplay
  ): Behavior[InternalCommand] =
    Behaviors.receiveMessage {
      case SmurfSelected(smurf, replyTo) =>
        //TODO send smurf to be accepted by the team leader
        smurfRelationSaving(metaInfoReplay)(replyTo)
      case _ => Behaviors.unhandled
    }

  private case class GameBySmurf(
      winnersOwner: Option[Either[String, Option[DiscordPlayerLogged]]] = None,
      loserssOwner: Option[Either[String, Option[DiscordPlayerLogged]]] = None,
      game: Option[OneVsOne] = None
  ) {
    val isComplete: Boolean =
      Seq(winnersOwner, loserssOwner, game).forall(_.isDefined)
    private def take(
        owner: Option[Either[String, Option[DiscordPlayerLogged]]]
    )(discordID: DiscordID): Option[DiscordPlayerLogged] =
      owner match {
        case Some(Right(Some(logged))) if logged.discordID == discordID =>
          Some(logged)
        case _ => None
      }
    private def takeIfWinner(discordID: DiscordID) =
      take(winnersOwner)(discordID)
    private def takeIfLoser(discordID: DiscordID) =
      take(loserssOwner)(discordID)
    def isAPlayer(discordID: DiscordID): Option[DiscordPlayerLogged] =
      takeIfWinner(discordID).orElse(takeIfLoser(discordID))
  }
  private case class MetaInfoReplay(
      id: ReplayTeamID,
      senderID: DiscordID,
      teamID: TeamID,
      replay: File
  )
  def awaitingResultOfSmurfs(
      metaInfoReplay: MetaInfoReplay,
      replyTo: ActorRef[Response],
      parent: ActorRef[TeamReplayManager.Command]
  ): Behavior[InternalCommand] =
    Behaviors.receiveMessage {
      case BothSmurfsFree(oneVsOne) =>
        replyTo ! SmurfMustBeSelectedResponse(oneVsOne)
        parent ! TeamReplayManager.AwaitSender(metaInfoReplay.id)
        awaitSmurfOfSender(metaInfoReplay)
      case SmurfSenderValid(oneVsOne, smurf) =>
        //TODO push replay info
        replayInfoSaving(metaInfoReplay)(replyTo)
    }
  def awaitingOwnersOfSmurfs(
      metaInfoReplay: MetaInfoReplay,
      gameBySmurf: GameBySmurf,
      replyTo: ActorRef[Response],
      parent: ActorRef[TeamReplayManager.Command]
  )(implicit
      judger: ActorRef[GameJudge.JudgeGame],
      uniqueSmurfWatcher: ActorRef[UniqueSmurfWatcher.Command]
  ): Behavior[InternalCommand] = {

    Behaviors.receive {
      case (ctx, message) =>
        val newGame = message match {
          case GameParsed(oneVsOne) => gameBySmurf.copy(game = Some(oneVsOne))
          case WinnerOwnerResponse(
                UniqueSmurfWatcher.UserOwner(discordPlayerLogged)
              ) =>
            gameBySmurf.copy(winnersOwner =
              Some(Right(Some(discordPlayerLogged)))
            )
          case WinnerOwnerResponse(
                UniqueSmurfWatcher.SmurfNotAssigned()
              ) =>
            gameBySmurf.copy(winnersOwner = Some(Right(None)))
          case WinnerOwnerResponse(
                UniqueSmurfWatcher.ErrorSmurfWatcher(reason)
              ) =>
            gameBySmurf.copy(winnersOwner = Some(Left(reason)))
          case LoserOwnerResponse(
                UniqueSmurfWatcher.UserOwner(discordPlayerLogged)
              ) =>
            gameBySmurf.copy(loserssOwner =
              Some(Right(Some(discordPlayerLogged)))
            )
          case LoserOwnerResponse(
                UniqueSmurfWatcher.SmurfNotAssigned()
              ) =>
            gameBySmurf.copy(loserssOwner = Some(Right(None)))
          case LoserOwnerResponse(
                UniqueSmurfWatcher.ErrorSmurfWatcher(reason)
              ) =>
            gameBySmurf.copy(loserssOwner = Some(Left(reason)))
        }

        if (newGame.isComplete) {
          val senderIsAPlayer = newGame.isAPlayer(metaInfoReplay.senderID)

          awaitingResultOfSmurfs(metaInfoReplay, replyTo, parent)
        } else {
          Behaviors.same
        }

    }
  }

  def checkingReplayStatus(
      metaInfoReplay: MetaInfoReplay,
      replyTo: ActorRef[Response],
      parent: ActorRef[TeamReplayManager.Command]
  )(implicit
      judger: ActorRef[GameJudge.JudgeGame],
      uniqueSmurfWatcher: ActorRef[UniqueSmurfWatcher.Command]
  ): Behavior[InternalCommand] =
    Behaviors.receive {
      case (_, BothSmurfsFree(oneVsOne)) =>
        replyTo ! SmurfMustBeSelectedResponse(oneVsOne)
        parent ! TeamReplayManager.AwaitSender(metaInfoReplay.id)
        awaitSmurfOfSender(metaInfoReplay)
      case (_, SmurfSenderValid(oneVsOne, smurf)) =>
        //TODO push replay info
        replayInfoSaving(metaInfoReplay)(replyTo)
      case (ctx, ReplayParsedMessage(replayParsed)) =>
        judger ! GameJudge.JudgeGame(
          replayParsed,
          ctx.messageAdapter {
            case onevsone @ OneVsOne(winner, loser) =>
              uniqueSmurfWatcher ! UniqueSmurfWatcher.LocateOwner(
                Smurf(winner.smurf),
                ctx.messageAdapter(WinnerOwnerResponse)
              )
              uniqueSmurfWatcher ! UniqueSmurfWatcher.LocateOwner(
                Smurf(loser.smurf),
                ctx.messageAdapter(LoserOwnerResponse)
              )
              GameParsed(onevsone)
            case _ =>
              ReplayErrorParsing("La replay no pudo interpretarse como 1vs1")
          }
        )
        awaitingOwnersOfSmurfs(
          metaInfoReplay,
          GameBySmurf(None, None, None),
          replyTo,
          parent
        )
      case (_, ReplayErrorParsing(reason)) =>
        replyTo ! SubmitError(reason)
        Behaviors.stopped
      case _ => Behaviors.unhandled
    }

  def checkDuplicatePending(
      metaInfoReplay: MetaInfoReplay,
      replyTo: ActorRef[Response],
      parent: ActorRef[TeamReplayManager.Command]
  )(implicit
      parseReplayFileService: ParseReplayFileService,
      judger: ActorRef[GameJudge.JudgeGame],
      uniqueSmurfWatcher: ActorRef[UniqueSmurfWatcher.Command]
  ): Behavior[InternalCommand] = {
    Behaviors.receive {
      case (ctx, Unique()) =>
        //TODO parse replay
        ctx.pipeToSelf(
          parseReplayFileService
            .parseFile(metaInfoReplay.replay)
            .map(GameInfo.apply)
        ) {
          case Success(parsed: ReplayParsed) => ???
          case Success(ImpossibleToParse)    => ???
          case Failure(error)                => ???
        }
        checkingReplayStatus(metaInfoReplay, replyTo, parent)
      case (_, Pending()) =>
        replyTo ! SubmitError("La replay está siendo procesada")
        Behaviors.stopped
      case (_, Duplicated()) =>
        replyTo ! SubmitError("La replay ya ha sido procesado")
        Behaviors.stopped
      case (_, ReplayErrorParsing(reason)) =>
        replyTo ! SubmitError(reason)
        Behaviors.stopped
      case _ => Behaviors.unhandled
    }
  }

  def apply(
      uniqueReplayWatcher: ActorRef[UniqueReplayWatcher.Command]
  )(implicit
      parseReplayFileService: ParseReplayFileService,
      judger: ActorRef[GameJudge.JudgeGame],
      uniqueSmurfWatcher: ActorRef[UniqueSmurfWatcher.Command]
  ): Behavior[Command] =
    Behaviors
      .receive[InternalCommand] {
        case (ctx, Submit(id, senderID, teamID, replay, replyTo, parent)) =>
          uniqueReplayWatcher ! UniqueReplayWatcher.Unique(
            ReplayRecord.md5HashString(replay),
            ctx.messageAdapter[UniqueReplayWatcher.UniqueResponse] {
              case UniqueReplayWatcher.IsUnique()    => Unique()
              case UniqueReplayWatcher.IsPending()   => Pending()
              case UniqueReplayWatcher.IsNotUnique() => Duplicated()
              case UniqueReplayWatcher.ErrorResponse(reason) =>
                ReplayErrorParsing(reason)
            }
          )
          checkDuplicatePending(
            MetaInfoReplay(id, senderID, teamID, replay),
            replyTo,
            parent
          )
        case _ => Behaviors.unhandled
      }
      .narrow
}
