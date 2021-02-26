package shared.models.teamsystem

import shared.models.ReplayTeamID
import shared.models.StarCraftModels.OneVsOne
import upickle.default.{macroRW, ReadWriter => RW}
import upickle.default._
case class TeamReplayResponse(typeMessage: String, content: String)

object TeamReplayResponse {
  def apply(
      specificTeamReplayResponse: SpecificTeamReplayResponse
  ): TeamReplayResponse =
    new TeamReplayResponse(
      specificTeamReplayResponse.typeMessage,
      specificTeamReplayResponse.content
    )
  implicit val teamReplayResponseRW: RW[TeamReplayResponse] = macroRW

}
sealed trait SpecificTeamReplayResponse {
  def typeMessage: String
  def content: String
}
object SpecificTeamReplayResponse {
  def apply(
      response: TeamReplayResponse
  ): Option[SpecificTeamReplayResponse] = {
    response.typeMessage match {
      case TeamReplayError.typeMessage =>
        Some(read[TeamReplayError](response.content))
      case ReplaySaved.typeMessage => Some(read[ReplaySaved](response.content))
      case SmurfToVerify.typeMessage =>
        Some(read[SmurfToVerify](response.content))
      case _ => None
    }
  }
}
case class TeamReplayError(reason: String) extends SpecificTeamReplayResponse {
  override val typeMessage: String = TeamReplayError.typeMessage
  override def content: String = write(this)
}
object TeamReplayError {
  implicit val teamReplayErrorRW: RW[TeamReplayError] = macroRW
  val typeMessage: String = "TeamReplayError"
}

case class ReplaySaved() extends SpecificTeamReplayResponse {
  override val typeMessage: String = ReplaySaved.typeMessage
  override val content: String = write(this)
}
object ReplaySaved {
  implicit val replaySavedRW: RW[ReplaySaved] = macroRW
  val typeMessage: String = "ReplaySaved"
}

case class SmurfToVerify(replayTeamID: ReplayTeamID, oneVsOne: OneVsOne)
    extends SpecificTeamReplayResponse {
  override val typeMessage: String = SmurfToVerify.typeMessage
  override val content: String = write(this)
}
object SmurfToVerify {
  implicit val smurfToVerifyRW: RW[SmurfToVerify] = macroRW
  val typeMessage: String = "SmurfToVerify"
}
