package models.daos.usertrajectory

import models.ReplayRecordResumen
import shared.models.StarCraftModels.SCRace

import scala.concurrent.Future

trait ReplayRecordResumenDAO {
  import ReplayRecordResumenDAO._
  def add(replayRecordResumen: ReplayRecordResumen): Future[ReplayRecordResumen]
  def load(param: SearchParam): Future[Seq[ReplayRecordResumen]]
  def update(
      replayRecordResumen: ReplayRecordResumen
  ): Future[Boolean]
}

object ReplayRecordResumenDAO {
  sealed trait SearchParam
  sealed trait Composition extends SearchParam
  case class AndComposition(left: SearchParam, right: SearchParam)
      extends Composition
  case class OrComposition(left: SearchParam, right: SearchParam)
      extends Composition

  case class ByPlayer(discordID: String) extends SearchParam
  case class ByPlayerName(discordName: String) extends SearchParam
  case class ByRace(race: SCRace) extends SearchParam
  case class ByMatch(first: SCRace, second: SCRace) extends SearchParam
}
