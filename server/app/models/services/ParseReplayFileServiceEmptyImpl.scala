package models.services

import java.io.File

import shared.models.ActionBySmurf.{ActionBySmurf, Correlated2d1rDefined, CorrelatedParallelDefined, SmurfsEmpty}
import shared.models.{ActionByReplay, ReplayDescriptionShared}

import scala.concurrent.Future

class ParseReplayFileServiceEmptyImpl extends ParseReplayFileService {
  def parseFile(file: File): Future[Either[String,String]] = Future.successful(Left("NOT IMPLEMENTED FUNCTION"))
  def parseFileAndBuildAction(file: File, discordUserID1: String, discordUserID2: String): Future[Either[String,ActionByReplay]] =
    Future.successful(Right(ActionByReplay(defined = true,Some("Flash"),Some("Bisu"),SmurfsEmpty,1,"LostTemple")))
}
