package models.services

import java.io.File

import jobs.{JobError, UnknowReplayPusherError}
import play.api.libs.json.{JsArray, Json}
import shared.models.{ActionByReplay, ReplayDescriptionShared}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

trait ParseReplayFileService {
  def parseFile(file: File): Future[Either[String,String]]
  def parseJsonResponse(stringJson: String): Either[String,ReplayDescriptionShared] = {

    val json = Json.parse(stringJson)
    val playersJson = (json \ "Header" \ "Players").getOrElse(JsArray.empty).asInstanceOf[JsArray]
    case class Player(team: Int, name: String)
    val players = playersJson.value.toList.flatMap{ p =>
      for{
        team <- (p \ "Team").asOpt[Int]
        name <- (p \ "Name").asOpt[String]
      }yield{
        Player(team,name)
      }

    }

    (for{
      p1 <- players.find(_.team ==1).map(_.name)
      p2 <- players.find(_.team ==2).map(_.name)
      winnerTeam <- (json \ "Computed" \ "WinnerTeam").asOpt[Int]
      mapName <- (json \ "Header" \ "Map").asOpt[String]
      startTime <- (json \ "Header" \ "StartTime").asOpt[String]
    }yield{
      Right(ReplayDescriptionShared(p1, p2, winnerTeam, mapName,Some(startTime)))
    }).getOrElse(Left("Cant find players"))

  }
  def parseFileAndBuildAction(file: File, discordUserID1: String, discordUserID2: String): Future[Either[String,ActionByReplay]]
  def parseFileAndBuildDescription(file: File): Future[Either[JobError, ReplayDescriptionShared]] = {
    (for{
      parsedEither <- parseFile(file)
    }yield{
      parsedEither.flatMap(parseJsonResponse)
    }).map{
      case Left(error) => Left(UnknowReplayPusherError(error))
      case Right(x) => Right(x)
    }


  }
}
