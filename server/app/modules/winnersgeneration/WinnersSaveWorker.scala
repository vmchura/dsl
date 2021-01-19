package modules.winnersgeneration

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}

object WinnersSaveWorker {
  trait InternalCommand
  case class TournamentSeasonSaved() extends InternalCommand
  def apply(
      replyTo: ActorRef[WinnersSaving.SaveWinnersCompleted],
      origin: ActorRef[WinnersSaving.WinnersSavingResponse]
  ): Behavior[InternalCommand] = {
    Behaviors.receiveMessage {
      case TournamentSeasonSaved() =>
        replyTo ! WinnersSaving.SaveWinnersCompleted(origin)
        Behaviors.stopped
    }
  }
}
