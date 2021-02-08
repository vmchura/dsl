package modules.teamsystem

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import models.Smurf
import shared.models.{DiscordID, ReplayTeamID}

import java.io.File

object TeamReplaySubmit {

  sealed trait InternalCommand
  sealed trait Command extends InternalCommand
  case class Submit(
      id: ReplayTeamID,
      senderID: DiscordID,
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

  def apply(): Behavior[Command] = Behaviors.unhandled
}
