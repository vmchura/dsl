package models

import org.joda.time.DateTime
import play.api.libs.json._

case class Match(matchPK: MatchPK,
                 firstChaNameID: Long, secondChaNameID: Long,
                 round: String, player1Name: Option[String], player2Name: Option[String], replaysAttached: Seq[ReplayRecord] = Nil) extends TWithReplays[Match]{
  def asMatchName(): MatchNameReplay = MatchNameReplay(round,
    player1Name.getOrElse("player1"),
    player2Name.getOrElse("player2"))
  override def withReplays(replays: Seq[ReplayRecord]): Match = copy(replaysAttached = replays)
}
case class MatchNameReplay(round: String, player1: String, player2: String){

  override def toString: String = {
    import MatchNameReplay.{stringToFillData => stfd}
    s"R_${stfd(round,6)}_${stfd(player1,10)}_${stfd(player2,10)}_${DateTime.now().millisOfDay().get()}.rep"
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



