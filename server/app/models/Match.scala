package models

import java.util.UUID

import org.joda.time.DateTime
import play.api.libs.json._

case class Match(matchPK: MatchPK,tournamentName: String,
                 firstChaNameID: Long, secondChaNameID: Long,
                 round: String, player1Name: Option[String], player2Name: Option[String], replaysAttached: Seq[ReplayRecord] = Nil) extends TWithReplays[Match]{
  def asMatchName(resultID: UUID): MatchNameReplay = MatchNameReplay(resultID, round,tournamentName,
    player1Name.getOrElse("player1"),
    player2Name.getOrElse("player2"))
  override def withReplays(replays: Seq[ReplayRecord]): Match = copy(replaysAttached = replays)
}
case class  MatchNameReplay(resultID: UUID, round: String,tournamentName: String, player1: String, player2: String){

  def uniqueNameReplay: String =  s"R_${resultID.toString}.rep"

  def pathFileOnCloud: String = {
    import MatchNameReplay.{stringToFillData => stfd}
    val group = round.filter(_.isLetter).mkString("")
    val ronda = round.filter(_.isDigit).mkString("")
    s"/$tournamentName/$group/R$ronda/R_${stfd(player1,8)}_${stfd(player2,8)}_${DateTime.now.getMillisOfDay}.rep"
  }

}
object MatchNameReplay{
  def stringToFillData(data: String,length: Int): String = {
    val validCharacters = data.filter(_.isLetterOrDigit)
    if(validCharacters.length >= length)
      validCharacters
    else{
      validCharacters + Array.fill(length - validCharacters.length)("-").mkString("")
    }
  }
}
object Match {
  implicit val jsonFormat: OFormat[Match] = Json.format[Match]
}
case class MatchPK(challongeID: Long,challongeMatchID: Long)
object MatchPK{
  implicit val jsonFormat: OFormat[MatchPK] = Json.format[MatchPK]
}



