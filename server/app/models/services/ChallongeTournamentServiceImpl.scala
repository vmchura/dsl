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
  override def findChallongeTournament(discordServerID: String)(tournamentUrlID: String): Future[Option[ChallongeTournament]] = {

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
          val tournamentModel = Tournament(chaID,discordServerID,name,active = false)
          case class ParticipantWithGroup(participant: Participant, groupIDs: Seq[Long])
          val participants = tournament("participants").as[JsArray].value.map(p => {
            val participant = Participant(ParticipantPK(chaID,p("participant")("id").as[Long]),p("participant")("name").as[String],None,None)
            val participan_group_ids = p("participant")("group_player_ids").as[JsArray].value.map(_.as[Long]).toSeq
            ParticipantWithGroup(participant,participan_group_ids)
          }).toSeq

          case class ChallongeMatch(match1v1: Match, round: Int, groupID: Option[Long])
          val matchesIncomplete = tournament("matches").as[JsArray].value.map(m => {
            val match1v1 = m("match")
            val round = match1v1("round").as[Int]
            val group_id = match1v1("group_id").asOpt[Long]
            val matchModel = Match(MatchPK(chaID,match1v1("id").as[Long]),match1v1("player1_id").as[Long],match1v1("player2_id").as[Long],"unknow")
            ChallongeMatch(matchModel,round, group_id)
          }).toSeq

          val mapGroup = matchesIncomplete.flatMap(_.groupID).distinct.sorted.zipWithIndex.map{
            case (g,i) => g -> ('A'.toInt + i).toChar.toString
          }.toMap
          def getMatchName(groupID: Option[Long],round: Int): String = {
            val groupOrBracket =  groupID.fold("Bracket")(g => mapGroup.getOrElse(g,"Bracket"))
            s"$groupOrBracket - $round"
          }
          def getChallongeUserID(player1ID: Long): Long = if(participants.map(_.participant.participantPK.chaNameID).contains(player1ID)) player1ID else participants.find(_.groupIDs.contains(player1ID)).map(_.participant.participantPK.chaNameID).getOrElse(player1ID)
          val matches = matchesIncomplete.map{ m =>
            val matchResult = m.match1v1
            matchResult.copy(firstChaNameID = getChallongeUserID(matchResult.firstChaNameID),
              secondChaNameID = getChallongeUserID(matchResult.secondChaNameID),
              round = getMatchName(m.groupID,m.round))
          }
          Some(ChallongeTournament(tournamentModel,participants.map(_.participant),matches))

        }catch{
          case _: Throwable =>
            logger.error(s"$body is not a challonge tournament")
            None
        }

      }
    }
  }
}
