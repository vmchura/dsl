package modules.winnersgeneration

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import com.google.inject.Provides
import models.services.TournamentSeriesService
import modules.winnersgeneration.WinnersSaveWorker.TournamentSeasonSaved
import play.api.libs.concurrent.ActorModule

import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global
object WinnersSaving extends ActorModule {

  trait WinnersSavingCommand
  case class SaveWinners(
      winnersInformation: WinnersInformation,
      replyTo: ActorRef[WinnersSavingResponse]
  ) extends WinnersSavingCommand

  case class SaveWinnersFailed(replyTo: ActorRef[WinnersSavingFailed])
      extends WinnersSavingCommand
  case class SaveWinnersCompleted(replyTo: ActorRef[WinnersSavedSuccessfully])
      extends WinnersSavingCommand

  trait WinnersSavingResponse
  case class WinnersSavedSuccessfully() extends WinnersSavingResponse
  case class WinnersSavingFailed() extends WinnersSavingResponse

  override type Message = WinnersSavingCommand
  @Provides
  def apply(
      tournamentSeriesService: TournamentSeriesService
  ): Behavior[WinnersSavingCommand] = {

    Behaviors.setup { ctx =>
      var children: Map[ActorRef[
        WinnersSavedSuccessfully with WinnersSavingFailed
      ], ActorRef[
        TournamentSeasonSaved
      ]] = Map.empty
      def removeChild(
          key: ActorRef[WinnersSavedSuccessfully with WinnersSavingFailed]
      )(actionIfPresent: => Unit): Unit = {
        children.get(key).foreach { worker =>
          ctx.unwatch(worker)
          children = children - key
          actionIfPresent
        }
      }
      Behaviors.receiveMessage {
        case SaveWinners(winnersInformation, replyTo) =>
          val worker = ctx.spawnAnonymous(
            WinnersSaveWorker(
              ctx.self,
              replyTo
            )
          )
          children = children + (replyTo -> worker)
          ctx.scheduleOnce(10 seconds, ctx.self, SaveWinnersFailed(replyTo))
          ctx.watchWith(worker, SaveWinnersFailed(replyTo))
          tournamentSeriesService
            .addSeason(
              winnersInformation.tournamentSeries,
              winnersInformation.tournamentID,
              winnersInformation.season,
              winnersInformation.players.map { case (pos, id) => (pos, id.id) }
            )
            .onComplete {
              case Success(true) => worker ! TournamentSeasonSaved()
              case Success(false) | Failure(_) =>
                ctx.self ! SaveWinnersFailed(replyTo)
            }
          Behaviors.same
        case SaveWinnersFailed(replyTo) =>
          removeChild(replyTo) {
            replyTo ! WinnersSavingFailed()
          }
          Behaviors.same
        case SaveWinnersCompleted(replyTo) =>
          removeChild(replyTo) {
            replyTo ! WinnersSavedSuccessfully()
          }
          Behaviors.same
      }

    }
  }
}
