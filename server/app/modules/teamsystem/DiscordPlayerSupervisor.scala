package modules.teamsystem

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import com.google.inject.Provides
import models.DiscordID
import play.api.libs.concurrent.ActorModule

import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

object DiscordPlayerSupervisor extends ActorModule {
  sealed trait DiscordPlayerSupervisorCommand
  case class Register(
      discordID: DiscordID,
      replyTo: ActorRef[DiscordPlayerWorker.DiscordPlayerWorkerResponse]
  ) extends DiscordPlayerSupervisorCommand
  case class StopWorker(
      worker: ActorRef[DiscordPlayerWorker.DiscordPlayerWorkerCommand]
  ) extends DiscordPlayerSupervisorCommand
  case class WorkerTerminated(
      worker: ActorRef[DiscordPlayerWorker.DiscordPlayerWorkerCommand]
  ) extends DiscordPlayerSupervisorCommand
  override type Message = DiscordPlayerSupervisorCommand

  @Provides
  def apply(discordPlayerWorker: DiscordPlayerWorker): Behavior[Message] =
    Behaviors.setup { ctx =>
      var workersAlive =
        Set.empty[ActorRef[DiscordPlayerWorker.DiscordPlayerWorkerCommand]]
      Behaviors.receiveMessage {
        case Register(discordID, replyTo) =>
          val worker =
            ctx.spawnAnonymous(discordPlayerWorker.initialBehavior(replyTo))
          workersAlive = workersAlive + worker

          worker ! DiscordPlayerWorker.Register(discordID)
          ctx.scheduleOnce(10 seconds, ctx.self, StopWorker(worker))
          ctx.watchWith(worker, WorkerTerminated(worker))
          Behaviors.same
        case StopWorker(worker) =>
          if (workersAlive.contains(worker))
            ctx.stop(worker)
          workersAlive = workersAlive - worker
          Behaviors.same
        case WorkerTerminated(worker) =>
          workersAlive = workersAlive - worker
          Behaviors.same
      }
    }

}
