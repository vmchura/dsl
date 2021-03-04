package modules.teamsystem

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import models.daos.teamsystem.{
  ReplayTeamDAO,
  TeamReplayDAO,
  TeamUserSmurfPendingDAO
}
import models.services.ParseReplayFileService
import models.{ReplayRecord, Smurf}
import models.teamsystem.{PendingSmurf, TeamID, TeamReplayInfo}
import modules.gameparser.GameJudge
import modules.gameparser.GameParser.{GameInfo, ImpossibleToParse, ReplayParsed}
import shared.models.StarCraftModels.OneVsOne
import shared.models.{
  DiscordID,
  DiscordPlayerLogged,
  ReplayTeamID,
  ReplayTeamRecord
}

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
  case class SmurfMustBeSelectedResponse(id: ReplayTeamID, oneVsOne: OneVsOne)
      extends Response

  case class Unique() extends InternalCommand
  case class Duplicated() extends InternalCommand
  case class Pending() extends InternalCommand
  case class BothSmurfsFree(oneVsOne: OneVsOne) extends InternalCommand
  case class OwnerResponse(
      oneVsOne: OneVsOne,
      response: UniqueSmurfWatcher.Response
  ) extends InternalCommand
  case class GameParsed(oneVsOne: OneVsOne) extends InternalCommand

  case class ReplayParsedMessage(replayParsed: ReplayParsed)
      extends InternalCommand
  case class ReplayErrorParsing(reason: String) extends InternalCommand
  case class SmurfSenderValid(oneVsOne: OneVsOne, smurf: Smurf)
      extends InternalCommand
  case class SmurfSenderToCheck(smurf: Smurf, oneVsOne: OneVsOne)
      extends InternalCommand

  case class SmurfSentToLeader(oneVsOne: OneVsOne) extends InternalCommand
  case class ReplayInfoSaved(teamReplayInfo: TeamReplayInfo)
      extends InternalCommand
  case class ReplaySaved() extends InternalCommand

  def saveReplayInfo(
      context: ActorContext[InternalCommand],
      metaInfoReplay: MetaInfoReplay,
      oneVsOne: OneVsOne
  )(implicit
      teamReplayDAO: TeamReplayDAO,
      replayTeamDAO: ReplayTeamDAO
  ): Unit = {

    context.pipeToSelf(
      {
        teamReplayDAO
          .save(
            TeamReplayInfo(
              metaInfoReplay.id,
              s"teamreplays/${metaInfoReplay.id.id.toString}.rep",
              metaInfoReplay.senderID
            )
          )
      }.flatMap { tri =>
        println(tri)
        replayTeamDAO
          .save(
            ReplayTeamRecord(
              metaInfoReplay.id,
              ReplayRecord.md5HashString(metaInfoReplay.replay),
              /* TODO name*/ "???",
              oneVsOne.startTime,
              org.joda.time.DateTime.now().toString("MM/dd/yyyy HH:mm:ss"),
              metaInfoReplay.senderID
            )
          )
          .map { res =>
            if (res) {
              tri
            } else {
              throw new IllegalStateException("Replay info hash no saved")
            }
          }
      }
    ) {
      case Success(teamReplayInfo) => ReplayInfoSaved(teamReplayInfo)
      case Failure(error) =>
        ReplayErrorParsing(s"Error saving InfoReplay: ${error.getMessage}")
    }
  }
  def replaySaving(implicit
      replyTo: ActorRef[Response]
  ): Behavior[InternalCommand] =
    Behaviors.receiveMessage {
      case ReplaySaved() =>
        replyTo ! ReplaySavedResponse()
        Behaviors.stopped
      case ReplayErrorParsing(reason) =>
        replyTo ! SubmitError(reason)
        Behaviors.stopped
      case _ =>
        replyTo ! SubmitError("Replay no se ha guardado satisfactoriamente")
        Behaviors.stopped
    }

  def replayInfoSaving(
      metaInfoReplay: MetaInfoReplay
  )(implicit
      replyTo: ActorRef[Response],
      pusher: ActorRef[FilePusherActor.Command]
  ): Behavior[InternalCommand] =
    Behaviors.receive {
      case (ctx, ReplayInfoSaved(_)) =>
        pusher ! FilePusherActor.Push(
          metaInfoReplay.replay,
          metaInfoReplay.id,
          ctx.messageAdapter {
            case FilePusherActor.Pushed()          => ReplaySaved()
            case FilePusherActor.PushError(reason) => ReplayErrorParsing(reason)
          }
        )
        replaySaving
      case (_, ReplayErrorParsing(reason)) =>
        replyTo ! SubmitError(reason)
        Behaviors.stopped
      case _ =>
        replyTo ! SubmitError(
          "Replay Info no se ha guardado satisfactoriamente"
        )
        Behaviors.stopped
    }

  def smurfRelationSaving(
      metaInfoReplay: MetaInfoReplay
  )(implicit
      replyTo: ActorRef[Response],
      teamReplyDAO: TeamReplayDAO,
      pusher: ActorRef[FilePusherActor.Command],
      replayTeamDAO: ReplayTeamDAO
  ): Behavior[InternalCommand] =
    Behaviors.receive {
      case (ctx, SmurfSentToLeader(oneVsOne)) =>
        saveReplayInfo(ctx, metaInfoReplay, oneVsOne)

        replayInfoSaving(metaInfoReplay)
      case _ =>
        replyTo ! SubmitError(
          "El smurf no ha podido enviarse al lider del equipo"
        )
        Behaviors.stopped
    }

  def sendSmurfToBeChecked(
      metaInfoReplay: MetaInfoReplay,
      oneVsOne: OneVsOne
  )(smurf: Smurf, ctx: ActorContext[InternalCommand])(implicit
      teamUserSmurfPendingDAO: TeamUserSmurfPendingDAO
  ): Unit =
    ctx.pipeToSelf(
      teamUserSmurfPendingDAO
        .add(PendingSmurf(metaInfoReplay.senderID, smurf, metaInfoReplay.id))
    ) {
      case Success(true) => SmurfSentToLeader(oneVsOne)
      case Success(false) =>
        ReplayErrorParsing("Can't save request of smurf")
      case Failure(exception) => ReplayErrorParsing(exception.getMessage)
    }

  def awaitSmurfOfSender(
      metaInfoReplay: MetaInfoReplay,
      oneVsOne: OneVsOne
  )(implicit
      teamReplyDAO: TeamReplayDAO,
      pusher: ActorRef[FilePusherActor.Command],
      teamUserSmurfPendingDAO: TeamUserSmurfPendingDAO,
      replayTeamDAO: ReplayTeamDAO
  ): Behavior[InternalCommand] =
    Behaviors.receive {
      case (ctx, SmurfSelected(smurf, replyTo)) =>
        sendSmurfToBeChecked(metaInfoReplay, oneVsOne)(smurf, ctx)
        smurfRelationSaving(metaInfoReplay)(
          replyTo,
          teamReplyDAO,
          pusher,
          replayTeamDAO
        )
      case _ => Behaviors.unhandled
    }

  private case class GameBySmurfComplete(
      winnersOwner: Either[String, Option[DiscordPlayerLogged]],
      loserssOwner: Either[String, Option[DiscordPlayerLogged]],
      game: OneVsOne
  ) {

    def isWinner(discordID: DiscordID): Boolean =
      winnersOwner match {
        case Right(Some(discordPlayerLogged)) =>
          discordPlayerLogged.discordID == discordID
        case _ => false
      }
    def isLoser(discordID: DiscordID): Boolean =
      winnersOwner match {
        case Right(Some(discordPlayerLogged)) =>
          discordPlayerLogged.discordID == discordID
        case _ => false
      }
    def isAPlayer(discordID: DiscordID): Boolean =
      isWinner(discordID) || isLoser(discordID)

    val bothPlayersCompleteCorrectly: Boolean =
      (winnersOwner, loserssOwner) match {
        case (Right(_), Right(_)) => true
        case _                    => false
      }
    def canNotBeAPlayer(discordID: DiscordID): Boolean =
      if (isAPlayer(discordID))
        false
      else {
        (winnersOwner, loserssOwner) match {
          case (Right(Some(_)), Right(Some(_))) => true
          case _                                => false
        }
      }
    def canFillSpace(
        discordID: DiscordID
    )(space: Either[String, Option[DiscordPlayerLogged]]): Boolean =
      if (isAPlayer(discordID))
        false
      else {
        space match {
          case Right(None) => true
          case _           => false
        }
      }
    def canFillToBeWinner(discordID: DiscordID): Boolean =
      canFillSpace(discordID)(winnersOwner)
    def canFillToBeLoser(discordID: DiscordID): Boolean =
      canFillSpace(discordID)(loserssOwner)
    def canFillBothSpaces(discordID: DiscordID): Boolean =
      canFillToBeWinner(discordID) && canFillToBeLoser(discordID)
  }
  private case class GameBySmurf(
      winnersOwner: Option[Either[String, Option[DiscordPlayerLogged]]] = None,
      loserssOwner: Option[Either[String, Option[DiscordPlayerLogged]]] = None,
      game: Option[OneVsOne] = None
  ) {
    val isComplete: Boolean =
      Seq(winnersOwner, loserssOwner, game).forall(_.isDefined)

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
  )(implicit
      teamReplayDAO: TeamReplayDAO,
      pusher: ActorRef[FilePusherActor.Command],
      teamUserSmurfPendingDAO: TeamUserSmurfPendingDAO,
      replayTeamDAO: ReplayTeamDAO
  ): Behavior[InternalCommand] =
    Behaviors.receive {
      case (_, BothSmurfsFree(oneVsOne)) =>
        replyTo ! SmurfMustBeSelectedResponse(metaInfoReplay.id, oneVsOne)
        parent ! TeamReplayManager.AwaitSender(metaInfoReplay.id)
        awaitSmurfOfSender(metaInfoReplay, oneVsOne)
      case (ctx, SmurfSenderValid(oneVsOne, _)) =>
        saveReplayInfo(ctx, metaInfoReplay, oneVsOne)
        replayInfoSaving(metaInfoReplay)(replyTo, pusher)
      case (ctx, SmurfSenderToCheck(smurf, oneVsOne)) =>
        sendSmurfToBeChecked(metaInfoReplay, oneVsOne)(smurf, ctx)
        smurfRelationSaving(metaInfoReplay)(
          replyTo,
          teamReplayDAO,
          pusher,
          replayTeamDAO
        )
      case (_, ReplayErrorParsing(reason)) =>
        replyTo ! SubmitError(reason)
        Behaviors.stopped
    }
  def awaitingOwnersOfSmurfs(
      metaInfoReplay: MetaInfoReplay,
      gameBySmurf: GameBySmurf,
      replyTo: ActorRef[Response],
      parent: ActorRef[TeamReplayManager.Command]
  )(implicit
      uniqueSmurfWatcher: ActorRef[UniqueSmurfWatcher.Command],
      teamReplayDAO: TeamReplayDAO,
      pusher: ActorRef[FilePusherActor.Command],
      teamUserSmurfPendingDAO: TeamUserSmurfPendingDAO,
      replayTeamDAO: ReplayTeamDAO
  ): Behavior[InternalCommand] = {

    Behaviors.receive {
      case (ctx, message) =>
        val newGame = message match {
          case GameParsed(oneVsOne) => gameBySmurf.copy(game = Some(oneVsOne))
          case OwnerResponse(
                oneVsOne @ OneVsOne(_, loser, _, _),
                UniqueSmurfWatcher.UserOwner(discordPlayerLogged)
              ) =>
            if (gameBySmurf.winnersOwner.isEmpty) {
              uniqueSmurfWatcher ! UniqueSmurfWatcher.LocateOwner(
                Smurf(loser.smurf),
                ctx.messageAdapter(response =>
                  OwnerResponse(oneVsOne, response)
                )
              )
              gameBySmurf
                .copy(winnersOwner = Some(Right(Some(discordPlayerLogged))))
            } else {
              gameBySmurf.copy(loserssOwner =
                Some(Right(Some(discordPlayerLogged)))
              )
            }

          case OwnerResponse(
                oneVsOne @ OneVsOne(_, loser, _, _),
                UniqueSmurfWatcher.SmurfNotAssigned()
              ) =>
            if (gameBySmurf.winnersOwner.isEmpty) {
              uniqueSmurfWatcher ! UniqueSmurfWatcher.LocateOwner(
                Smurf(loser.smurf),
                ctx.messageAdapter(response =>
                  OwnerResponse(oneVsOne, response)
                )
              )
              gameBySmurf.copy(winnersOwner = Some(Right(None)))
            } else {
              gameBySmurf.copy(loserssOwner = Some(Right(None)))
            }
          case OwnerResponse(
                oneVsOne @ OneVsOne(_, loser, _, _),
                UniqueSmurfWatcher.ErrorSmurfWatcher(reason)
              ) =>
            if (gameBySmurf.winnersOwner.isEmpty) {
              uniqueSmurfWatcher ! UniqueSmurfWatcher.LocateOwner(
                Smurf(loser.smurf),
                ctx.messageAdapter(response =>
                  OwnerResponse(oneVsOne, response)
                )
              )
              gameBySmurf.copy(winnersOwner = Some(Left(reason)))
            } else {
              gameBySmurf.copy(loserssOwner = Some(Left(reason)))
            }
        }

        if (newGame.isComplete) {
          val completeGame = GameBySmurfComplete(
            newGame.winnersOwner.get,
            newGame.loserssOwner.get,
            newGame.game.get
          )
          val messageToSend =
            if (completeGame.isAPlayer(metaInfoReplay.senderID)) {
              SmurfSenderValid(
                completeGame.game, {
                  if (completeGame.isWinner(metaInfoReplay.senderID))
                    Smurf(completeGame.game.winner.smurf)
                  else {
                    Smurf(completeGame.game.loser.smurf)
                  }
                }
              )
            } else {
              if (completeGame.canNotBeAPlayer(metaInfoReplay.senderID)) {
                ReplayErrorParsing("smurfs ya pertenecen a otros usuarios")
              } else {
                if (completeGame.bothPlayersCompleteCorrectly) {
                  if (completeGame.canFillBothSpaces(metaInfoReplay.senderID)) {
                    BothSmurfsFree(completeGame.game)
                  } else {
                    if (
                      completeGame.canFillToBeWinner(metaInfoReplay.senderID)
                    ) {
                      SmurfSenderToCheck(
                        Smurf(completeGame.game.winner.smurf),
                        completeGame.game
                      )
                    } else {
                      if (
                        completeGame.canFillToBeLoser(metaInfoReplay.senderID)
                      ) {
                        SmurfSenderToCheck(
                          Smurf(completeGame.game.loser.smurf),
                          completeGame.game
                        )
                      } else {
                        ReplayErrorParsing("Error en la lógica del sistema")
                      }
                    }
                  }
                } else {
                  ReplayErrorParsing("Error al obtener el jugador de un smurf")
                }
              }
            }
          ctx.self ! messageToSend
          awaitingResultOfSmurfs(metaInfoReplay, replyTo, parent)
        } else {
          awaitingOwnersOfSmurfs(
            metaInfoReplay,
            newGame,
            replyTo,
            parent
          )
        }

    }
  }

  def checkingReplayStatus(
      metaInfoReplay: MetaInfoReplay,
      replyTo: ActorRef[Response],
      parent: ActorRef[TeamReplayManager.Command]
  )(implicit
      uniqueSmurfWatcher: ActorRef[UniqueSmurfWatcher.Command],
      teamReplayDAO: TeamReplayDAO,
      pusher: ActorRef[FilePusherActor.Command],
      teamUserSmurfPendingDAO: TeamUserSmurfPendingDAO,
      replayTeamDAO: ReplayTeamDAO
  ): Behavior[InternalCommand] =
    Behaviors.receive {
      case (ctx, ReplayParsedMessage(replayParsed)) =>
        val judger = ctx.spawnAnonymous(GameJudge())
        judger ! GameJudge.JudgeGame(
          replayParsed,
          ctx.messageAdapter {
            case onevsone @ OneVsOne(winner, _, _, _) =>
              uniqueSmurfWatcher ! UniqueSmurfWatcher.LocateOwner(
                Smurf(winner.smurf),
                ctx.messageAdapter(response =>
                  OwnerResponse(onevsone, response)
                )
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
      case _ =>
        replyTo ! SubmitError("Error en el sistema")
        Behaviors.stopped
    }

  def checkDuplicatePending(
      metaInfoReplay: MetaInfoReplay,
      replyTo: ActorRef[Response],
      parent: ActorRef[TeamReplayManager.Command]
  )(implicit
      parseReplayFileService: ParseReplayFileService,
      uniqueSmurfWatcher: ActorRef[UniqueSmurfWatcher.Command],
      teamReplayDAO: TeamReplayDAO,
      pusher: ActorRef[FilePusherActor.Command],
      teamUserSmurfPendingDAO: TeamUserSmurfPendingDAO,
      replayTeamDAO: ReplayTeamDAO
  ): Behavior[InternalCommand] = {
    Behaviors.receive {
      case (ctx, Unique()) =>
        ctx.pipeToSelf(
          parseReplayFileService
            .parseFile(metaInfoReplay.replay)
            .map(GameInfo.apply)
        ) {
          case Success(parsed: ReplayParsed) => ReplayParsedMessage(parsed)
          case Success(ImpossibleToParse) =>
            ReplayErrorParsing("Impossible to parse replay")
          case Failure(error) =>
            ReplayErrorParsing(s"Error parsing file: ${error.getMessage}")
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
      case (_, _) =>
        Behaviors.unhandled
    }
  }

  def apply(
      uniqueReplayWatcher: ActorRef[UniqueReplayWatcher.Command]
  )(implicit
      parseReplayFileService: ParseReplayFileService,
      uniqueSmurfWatcher: ActorRef[UniqueSmurfWatcher.Command],
      teamReplayDAO: TeamReplayDAO,
      pusher: ActorRef[FilePusherActor.Command],
      teamUserSmurfPendingDAO: TeamUserSmurfPendingDAO,
      replayTeamDAO: ReplayTeamDAO
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
        case _ =>
          Behaviors.unhandled
      }
      .narrow
}
