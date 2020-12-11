package modules.gameparser

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import models.services.ParseReplayFileService
import models.StarCraftModels._
import play.api.libs.json.{JsArray, Json}

import java.io.File
import scala.util.{Failure, Success}

object GameParser {
  sealed trait ReplayMetaData

  case class ReplayToParse(replay: File, replyTo: ActorRef[GameInfo])
      extends ReplayMetaData
  case class ReplayJsonParsed(
      data: Either[String, String],
      replyTo: ActorRef[GameInfo]
  ) extends ReplayMetaData

  case class ReplayCannotBeParsed(replyTo: ActorRef[GameInfo])
      extends ReplayMetaData

  case class Team(index: Int, participants: List[SCPlayer])

  sealed trait GameInfo
  case class ReplayParsed(
      gameMode: SCMatchMode,
      teams: List[Team],
      winnerTeamIndex: Int
  ) extends GameInfo
  case object ImpossibleToParse extends GameInfo
  object GameInfo {
    def apply(data: Either[String, String]): GameInfo =
      data match {
        case Left(_) => ImpossibleToParse
        case Right(jsonStr) =>
          val json = Json.parse(jsonStr)
          val playersJson = (json \ "Header" \ "Players")
            .getOrElse(JsArray.empty)
            .asInstanceOf[JsArray]
          val players = playersJson.value.toList
            .flatMap { p =>
              for {
                _ <- (p \ "Type" \ "Name").asOpt[String].flatMap {
                  case "Human" => Some(true)
                  case _       => None
                }
                team <- (p \ "Team").asOpt[Int]
                name <- (p \ "Name").asOpt[String]
                race <- {
                  (p \ "Race" \ "Name").asOpt[String].flatMap {
                    case "Zerg"    => Some(Zerg)
                    case "Terran"  => Some(Terran)
                    case "Protoss" => Some(Protoss)
                    case _         => None
                  }
                }
              } yield {
                (team, SCPlayer(name, race))
              }

            }
            .groupBy(_._1)
            .map { case (team, teamPlayers) => (team, teamPlayers.map(_._2)) }
            .toList
            .map { case (i, players) => Team(i, players) }

          val gameMode: SCMatchMode = {
            (json \ "Header" \ "Type" \ "ShortName").asOpt[String] match {
              case Some("TvB")   => TopVsBottom
              case Some("Melee") => Melee
              case Some("1v1")   => OneVsOneMode
              case _             => UnknownMode
            }
          }
          (json \ "Computed" \ "WinnerTeam").asOpt[Int] match {
            case Some(winnerTeam) =>
              ReplayParsed(
                gameMode,
                players,
                winnerTeam
              )
            case None => ImpossibleToParse
          }

      }
  }

  def apply(
      parseReplayFileService: ParseReplayFileService
  ): Behavior[ReplayMetaData] = {
    Behaviors.setup[ReplayMetaData] { context =>
      val lastBehaviour = {
        Behaviors.receiveMessage[ReplayMetaData] { message =>
          message match {
            case ReplayJsonParsed(data, replyTo) =>
              replyTo ! GameInfo(data)
            case ReplayCannotBeParsed(replyTo) =>
              replyTo ! ImpossibleToParse
            case _ =>
              throw new IllegalStateException("bad message at GameParser")
          }
          Behaviors.stopped
        }
      }

      val initBehaviour = Behaviors.receiveMessage[ReplayMetaData] { message =>
        message match {
          case ReplayToParse(replay, replyTo) =>
            val futureParsed = parseReplayFileService.parseFile(replay)
            context.pipeToSelf(futureParsed) {
              case Success(value) => ReplayJsonParsed(value, replyTo)
              case Failure(_)     => ReplayCannotBeParsed(replyTo)
            }
          case _ => throw new IllegalStateException("bad message at GameParser")
        }
        lastBehaviour
      }

      initBehaviour
    }

  }
}
