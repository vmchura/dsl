package models

import java.util.UUID
import play.api.libs.json._

case class Match(matchPK: MatchPK, firstChaNameID: String, secondChaNameID: String, round: String)
object Match {
  implicit val jsonFormat: OFormat[Match] = Json.format[Match]
}
case class MatchPK(tournamentID: UUID,challongeMatchID: String)
object MatchPK{
  implicit val jsonFormat: OFormat[MatchPK] = Json.format[MatchPK]
}



