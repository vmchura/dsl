package models.services

import models.{ChallongeTournament, Match, MatchPK, Participant, ParticipantPK, Tournament}

import scala.concurrent.Future
import sttp.client._
import sttp.client.asynchttpclient.WebSocketHandler
import sttp.client.asynchttpclient.future.AsyncHttpClientFutureBackend
import utils.Logger
import play.api.libs.json._
import javax.inject.Inject
import play.api.Configuration
import scala.concurrent.ExecutionContext.Implicits.global
class ChallongeTournamentServiceImpl @Inject()(configuration: Configuration) extends ChallongeTournamentService with Logger {
  implicit val sttpBackend: SttpBackend[Future, Nothing, WebSocketHandler] = AsyncHttpClientFutureBackend()
  override protected val challongeApiKey: String = configuration.get[String]("challonge.apikey")
  override def findChallongeTournament(discordServerID: String,discordChanelReplayID: Option[String] = None)(tournamentUrlID: String): Future[Option[ChallongeTournament]] = {

    val responseFut = basicRequest.get(uri"https://api.challonge.com/v1/tournaments/$tournamentUrlID.json?api_key=$challongeApiKey&include_participants=1&include_matches=1").send()
    responseFut.map{ _.body match {
      case Left(errorMessage) =>
        logger.error(errorMessage)
        None
      case Right(body) =>
        try{
          val tournament = Json.parse(body)("tournament")
          val chaID = tournament("id").as[Long]
          val name = tournament("name").as[String]
          val tournamentModel = Tournament(chaID,tournamentUrlID,discordServerID,name,active = false,discordChanelReplayID)
          case class ParticipantWithGroup(participant: Participant, groupIDs: Seq[Long])
          val participants = tournament("participants").as[JsArray].value.map(p => {
            val participant = Participant(ParticipantPK(chaID,p("participant")("id").as[Long]),p("participant")("name").as[String],None,None)
            val participan_group_ids = p("participant")("group_player_ids").as[JsArray].value.map(_.as[Long]).toSeq
            ParticipantWithGroup(participant,participan_group_ids)
          }).toSeq

          def findParticipantBySomeID(id: Long): Option[Participant] = {
            participants.find(p => p.participant.participantPK.chaNameID == id || p.groupIDs.contains(id)).map(_.participant)

          }
          case class ChallongeMatch(match1v1: Match, round: Int, groupID: Option[Long], identifier: String)
          val matchesIncomplete = tournament("matches").as[JsArray].value.flatMap(m => {
            val match1v1 = m("match")
            val round = match1v1("round").as[Int]
            val group_id = match1v1("group_id").asOpt[Long]
            val identifier = match1v1("identifier").as[String]
            val state = match1v1("state").as[String]
            for{
              playerID1 <- match1v1("player1_id").asOpt[Long]
              playerID2 <- match1v1("player2_id").asOpt[Long]
              match1v1ID <- match1v1("id").asOpt[Long]
            }yield{
              val matchModel = Match(MatchPK(chaID,match1v1ID),name,
                playerID1,
                playerID2,"unknow",
                findParticipantBySomeID(playerID1).map(_.chaname),
                findParticipantBySomeID(playerID2).map(_.chaname),
                state.equals("complete")
              )
              ChallongeMatch(matchModel,round, group_id,identifier)
            }


          }).toSeq

          val mapIfDefined: Map[Long,String] = matchesIncomplete.flatMap(_.groupID).distinct.sorted.zipWithIndex.map{
            case (g,i) => g -> ('A'.toInt + i).toChar.toString
          }.toMap
          def getMatchName(idOpt: Option[Long], round: Int, identifier: String): String = {
            idOpt match {
              case Some(id) => s"${mapIfDefined(id)} - $round"
              case None =>
                val countSimilar = matchesIncomplete.count(m => m.groupID.isEmpty && m.round == round)
                val identifiersSimilar: Map[String, String] = {
                  val ids = matchesIncomplete.filter(m => m.groupID.isEmpty && m.round == round).map(_.identifier).sorted
                  ids.zipWithIndex.map{case(id, i) => id -> ('1'.toChar+i).toChar.toString}.toMap
                }
                val has3OnIdentifier = identifier.contains('3')

                (countSimilar, has3OnIdentifier) match {
                  case (_, true) => s"TercerPuesto - 1"
                  case (1,false) => s"Finales - 1"
                  case (2,false) => s"Semifinales - ${identifiersSimilar.getOrElse(identifier,"1")}"
                  case (4,false) => s"Cuartos - ${identifiersSimilar.getOrElse(identifier,"1")}"
                  case (8,false) => s"Octavos - ${identifiersSimilar.getOrElse(identifier,"1")}"
                  case _ => s"Brackets - ${identifiersSimilar.getOrElse(identifier,"1")}"
                }
            }
          }
          def getChallongeUserID(player1ID: Long): Long = if(participants.map(_.participant.participantPK.chaNameID).contains(player1ID)) player1ID else participants.find(_.groupIDs.contains(player1ID)).map(_.participant.participantPK.chaNameID).getOrElse(player1ID)
          val matches = matchesIncomplete.map{ m =>
            val matchResult = m.match1v1
            matchResult.copy(firstChaNameID = getChallongeUserID(matchResult.firstChaNameID),
              secondChaNameID = getChallongeUserID(matchResult.secondChaNameID),
              round = getMatchName(m.groupID,m.round,m.identifier))
          }
          Some(ChallongeTournament(tournamentModel,participants.map(_.participant),matches))

        }catch{
          case e: Throwable =>
            logger.error(s"error triggered: ${e.toString}")
            logger.error(s"$body is not a challonge tournament")
            None
        }

      }
    }
  }
}
