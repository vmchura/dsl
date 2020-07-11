package models

import play.api.libs.json._

case class Match(matchPK: MatchPK, firstChaNameID: Long, secondChaNameID: Long, round: String)
object Match {
  implicit val jsonFormat: OFormat[Match] = Json.format[Match]
}
case class MatchPK(challongeID: Long,challongeMatchID: Long)
object MatchPK{
  implicit val jsonFormat: OFormat[MatchPK] = Json.format[MatchPK]
}



