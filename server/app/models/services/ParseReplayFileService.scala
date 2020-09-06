package models.services

import java.io.File

import play.api.libs.json.{JsArray, Json}
import shared.models.{ActionByReplay, ReplayDescriptionShared}

import scala.concurrent.Future

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
    }yield{
      Right(ReplayDescriptionShared(p1, p2, winnerTeam, mapName))
    }).getOrElse(Left("Cant find players"))

  }
  def parseFileAndBuildAction(file: File, discordUserID1: String, discordUserID2: String): Future[Either[String,ActionByReplay]]
}
