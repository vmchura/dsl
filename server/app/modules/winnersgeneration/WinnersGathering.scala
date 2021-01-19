package modules.winnersgeneration

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import com.google.inject.Provides
import models.daos.UserGuildDAO
import models.services.{TournamentSeriesService, TournamentService}
import modules.winnersgeneration.WinnersGatheringWorker.{
  GatheredInformationBuilder,
  InternalCommand
}
import play.api.libs.concurrent.ActorModule

import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
object WinnersGathering extends ActorModule {
  trait WinnersGatheringCommand
  case class Gather(replyTo: ActorRef[WinnersGatheringResponse])
      extends WinnersGatheringCommand
  case class PartialGatheringFailed(replyTo: ActorRef[GatheringFail])
      extends WinnersGatheringCommand
  case class GatheringComplete(
      replyTo: ActorRef[GatheringSucess],
      gatheredInformation: GatheredInformation
  ) extends WinnersGatheringCommand

  trait WinnersGatheringResponse
  case class GatheringSucess(gatheredInformation: GatheredInformation)
      extends WinnersGatheringResponse
  case class GatheringFail() extends WinnersGatheringResponse

  override type Message = WinnersGatheringCommand

  @Provides
  def apply(
      tournamentService: TournamentService,
      tournamentSeriesService: TournamentSeriesService,
      userGuildDAO: UserGuildDAO
  ): Behavior[WinnersGatheringCommand] = {
    Behaviors.setup { ctx =>
      var children: Map[ActorRef[GatheringSucess with GatheringFail], ActorRef[
        InternalCommand
      ]] =
        Map.empty
      def removeChild(
          key: ActorRef[GatheringSucess with GatheringFail]
      )(actionIfPresent: => Unit): Unit = {
        children.get(key).foreach { worker =>
          ctx.unwatch(worker)
          children = children - key
          actionIfPresent
        }
      }
      Behaviors.receiveMessage {
        case Gather(replyTo) =>
          val worker = ctx.spawnAnonymous(
            WinnersGatheringWorker(
              GatheredInformationBuilder(),
              ctx.self,
              replyTo
            )
          )
          children = children + (replyTo -> worker)
          ctx.scheduleOnce(
            10 seconds,
            ctx.self,
            PartialGatheringFailed(replyTo)
          )
          ctx.watchWith(worker, PartialGatheringFailed(replyTo))

          def processFuture[T](f: Future[T], g: T => InternalCommand): Unit = {
            f.onComplete {
              case Success(result) => worker ! g(result)
              case Failure(_)      => ctx.self ! PartialGatheringFailed(replyTo)
            }
          }

          processFuture(
            tournamentService.findAllTournaments(),
            WinnersGatheringWorker.TournamentsGathered
          )
          processFuture(
            tournamentSeriesService.allSeries(),
            WinnersGatheringWorker.TournamentSeriesGathered
          )
          processFuture(
            userGuildDAO.all(),
            WinnersGatheringWorker.UsersGathered
          )

          Behaviors.same

        case PartialGatheringFailed(replyTo: ActorRef[GatheringFail]) =>
          removeChild(replyTo) {
            replyTo ! GatheringFail()
          }
          Behaviors.same
        case GatheringComplete(replyTo, gatheredInformation) =>
          removeChild(replyTo) {
            replyTo ! GatheringSucess(gatheredInformation)
          }
          Behaviors.same
      }
    }
  }
}
