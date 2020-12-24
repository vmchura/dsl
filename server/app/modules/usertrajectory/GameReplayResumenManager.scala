package modules.usertrajectory

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import com.google.inject.Provides
import models.ReplayRecordResumen
import models.daos.usertrajectory.ReplayRecordResumenDAO
import play.api.libs.concurrent.ActorModule

object GameReplayResumenManager extends ActorModule {
  override type Message = MessageReplayResumen
  sealed trait MessageReplayResumen
  case class ReplayAdded(replayRecordResumen: ReplayRecordResumen)
      extends MessageReplayResumen
  case class ReplayUpdated(replayRecordResumen: ReplayRecordResumen)
      extends MessageReplayResumen
  case class ReplayDisabled(replayRecordResumen: ReplayRecordResumen)
      extends MessageReplayResumen
  @Provides
  def create(
      replayRecordResumenDAO: ReplayRecordResumenDAO
  ): Behavior[MessageReplayResumen] = {
    Behaviors.receiveMessage[MessageReplayResumen] { message =>
      {
        message match {
          case ReplayAdded(resumen) =>
            replayRecordResumenDAO.add(resumen)
          case ReplayUpdated(resumen) =>
            replayRecordResumenDAO.update(resumen)
          case ReplayDisabled(resumen) =>
            replayRecordResumenDAO.update(resumen.copy(enabled = false))
        }
      }
      Behaviors.same
    }
  }
}
