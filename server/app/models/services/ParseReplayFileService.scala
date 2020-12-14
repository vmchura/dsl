package models.services

import java.io.File

import jobs.{JobError, UnknowReplayPusherError}
import play.api.libs.json.{JsArray, Json}
import shared.models.{ActionByReplay, ReplayDescriptionShared}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

trait ParseReplayFileService {
  def parseFile(file: File): Future[Either[String, String]]
  def parseJsonResponse(
      stringJson: String
  ): Either[String, ReplayDescriptionShared] = {

    val json = Json.parse(stringJson)
    val playersJson = (json \ "Header" \ "Players")
      .getOrElse(JsArray.empty)
      .asInstanceOf[JsArray]
    case class Player(team: Int, name: String)
    val players = playersJson.value.toList
      .flatMap { p =>
        for {
          team <- (p \ "Team").asOpt[Int]
          name <- (p \ "Name").asOpt[String]
        } yield {
          Player(team, name)
        }

      }
      .sortBy(_.team)

    (for {
      _ <- if (players.length == 2) Some(2) else None
      p1 <- players.headOption.map(_.name)
      p2 <- players.tail.headOption.map(_.name)
      winnerTeam <- (json \ "Computed" \ "WinnerTeam").asOpt[Int]
      mapName <- (json \ "Header" \ "Map").asOpt[String]
      startTime <- (json \ "Header" \ "StartTime").asOpt[String]
    } yield {
      Right(
        ReplayDescriptionShared(
          p1,
          p2,
          if (winnerTeam == 1) winnerTeam else 2,
          mapName,
          Some(startTime)
        )
      )
    }).getOrElse(Left("Cant find players"))

  }

}
