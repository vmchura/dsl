package modules.teamsystem

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import com.google.inject.Provides
import models.Smurf
import models.daos.teamsystem.{
  TeamDAO,
  TeamMetaReplayTeamDAO,
  TeamReplayDAO,
  TeamUserSmurfPendingDAO
}
import models.services.ParseReplayFileService
import models.teamsystem.TeamID
import play.api.libs.concurrent.ActorModule
import shared.models.{DiscordID, ReplayTeamID}

import java.io.File
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Success
object TeamReplayManager extends ActorModule {
  sealed trait InternalCommand
  sealed trait Command extends InternalCommand
  case object IgnoredCommand extends InternalCommand
  case class Submit(
      senderID: DiscordID,
      teamID: TeamID,
      replay: File,
      replyTo: ActorRef[TeamReplaySubmit.Response]
  ) extends Command
  case class SubmitByTournament(
      senderID: DiscordID,
      replay: File
  ) extends Command
  case class SubmitByTournamentWithTeam(
      senderID: DiscordID,
      teamID: TeamID,
      replay: File
  ) extends Command
  case class SmurfSelected(
      replayTeamID: ReplayTeamID,
      smurf: Smurf,
      replyTo: ActorRef[TeamReplaySubmit.Response]
  ) extends Command
  case class SenderTimeOut(
      replayTeamID: ReplayTeamID,
      replyTo: ActorRef[TeamReplaySubmit.Response]
  ) extends Command
  case class AwaitSender(
      replayTeamID: ReplayTeamID
  ) extends Command

  case class WorkerDone(
      replayTeamID: ReplayTeamID
  ) extends InternalCommand
  override type Message = Command

  @Provides
  def apply()(implicit
      parseReplayFileService: ParseReplayFileService,
      uniqueReplayWatcher: ActorRef[UniqueReplayWatcher.Command],
      uniqueSmurfWatcher: ActorRef[UniqueSmurfWatcher.Command],
      teamReplayDAO: TeamReplayDAO,
      pusherFileActor: ActorRef[FilePusherActor.Command],
      teamUserSmurfPendingDAO: TeamUserSmurfPendingDAO,
      replayTeamDAO: TeamMetaReplayTeamDAO,
      pointsGenerator: ActorRef[PointsGenerator.Command],
      teamDAO: TeamDAO
  ): Behavior[Command] = {

    Behaviors
      .setup[InternalCommand] { ctx =>
        var current: Map[ReplayTeamID, ActorRef[TeamReplaySubmit.Command]] =
          Map.empty
        var awaiting: Map[ReplayTeamID, ActorRef[TeamReplaySubmit.Command]] =
          Map.empty
        Behaviors.receiveMessage {
          case Submit(senderID, teamID, replay, replyTo) =>
            val worker =
              ctx.spawnAnonymous(TeamReplaySubmit(uniqueReplayWatcher))
            val newID = ReplayTeamID()
            current = current + (newID -> worker)
            worker ! TeamReplaySubmit.Submit(
              newID,
              senderID,
              teamID,
              replay,
              replyTo,
              ctx.self
            )
            ctx.watchWith(worker, WorkerDone(newID))
            ctx.scheduleOnce(3 minutes, ctx.self, SenderTimeOut(newID, replyTo))
            Behaviors.same
          case SubmitByTournament(senderID, replay) =>
            ctx.pipeToSelf(
              teamDAO.teamsOf(senderID).map(_.find(_.isOfficial(senderID)))
            ) {
              case Success(Some(team)) =>
                SubmitByTournamentWithTeam(senderID, team.teamID, replay)
              case _ => IgnoredCommand
            }
            Behaviors.same
          case SubmitByTournamentWithTeam(senderID, teamID, replay) =>
            val worker =
              ctx.spawnAnonymous(TeamReplaySubmit(uniqueReplayWatcher))
            val newID = ReplayTeamID()
            current = current + (newID -> worker)
            worker ! TeamReplaySubmit.SubmitByTournament(
              newID,
              senderID,
              teamID,
              replay,
              ctx.system.ignoreRef,
              ctx.system.ignoreRef
            )
            ctx.watchWith(worker, WorkerDone(newID))
            ctx.scheduleOnce(
              3 minutes,
              ctx.self,
              SenderTimeOut(newID, ctx.system.ignoreRef)
            )
            Behaviors.same
          case SmurfSelected(replayTeamID, smurf, replyTo) =>
            awaiting.get(replayTeamID) match {
              case Some(worker) =>
                worker ! TeamReplaySubmit.SmurfSelected(smurf, replyTo)
              case None =>
                replyTo ! TeamReplaySubmit.SubmitError(
                  "Demora en contestar, intenta nuevamente"
                )
            }
            Behaviors.same
          case SenderTimeOut(replayTeamID, replyTo) =>
            current.get(replayTeamID).foreach(ctx.stop)
            awaiting = awaiting - replayTeamID
            current = current - replayTeamID
            replyTo ! TeamReplaySubmit.SubmitError(
              "Mucho tiempo de operación, error en el sistema"
            )
            Behaviors.same
          case AwaitSender(replayTeamID) =>
            awaiting = current
              .get(replayTeamID)
              .fold(awaiting)(worker => awaiting + (replayTeamID -> worker))
            Behaviors.same
          case WorkerDone(replayTeamID) =>
            awaiting = awaiting - replayTeamID
            current = current - replayTeamID
            Behaviors.same
          case IgnoredCommand => Behaviors.same
        }
      }
      .narrow
  }
}
