package shared.models
import shared.models.StarCraftModels._
import upickle.default.{macroRW, ReadWriter => RW}
case class DiscordIDSourceDefined(discordID: Either[String, String]) {
  val discordIDValue: String = discordID match {
    case Left(smurf)  => smurf
    case Right(smurf) => smurf
  }
  val withSource: DiscordIDWithSourceDefined = discordID match {
    case Left(smurf)  => DiscordByLogic(smurf)
    case Right(smurf) => DiscordByHistory(smurf)

  }

}
sealed trait DiscordIDWithSourceDefined
case class DiscordByHistory(discordID: String)
    extends DiscordIDWithSourceDefined
case class DiscordByLogic(discordID: String) extends DiscordIDWithSourceDefined

object DiscordIDSourceDefined {
  def buildByHistory(smurf: String): DiscordIDSourceDefined =
    DiscordIDSourceDefined(Right(smurf))
  def buildByLogic(smurf: String): DiscordIDSourceDefined =
    DiscordIDSourceDefined(Left(smurf))
}
case class ChallongePlayerDefined(
    discordID: DiscordIDSourceDefined,
    player: SCPlayer
)
case class ChallongeOneVsOneDefined(
    winner: ChallongePlayerDefined,
    loser: ChallongePlayerDefined
)

case class DiscordIDSource(
    discordID: Either[Option[String], Option[String]]
) {
  val isByHistory: Boolean = discordID.isRight
  val isByLogic: Boolean = discordID.isLeft
  val byHistory: Option[String] = discordID match {
    case Right(Some(smurf)) => Some(smurf)
    case _                  => None
  }
  val byLogic: Option[String] = discordID match {
    case Left(Some(smurf)) => Some(smurf)
    case _                 => None
  }
  val isEmpty: Boolean = if (isByHistory) byHistory.isEmpty else byLogic.isEmpty
  val withSource: DiscordIDWithSource =
    if (isEmpty) EmptyDiscordID
    else {
      (byHistory, byLogic) match {
        case (Some(h), _) => ByHistoryDiscordID(h)
        case (_, Some(l)) => ByLogicDiscordID(l)
        case _            => EmptyDiscordID
      }
    }
}
sealed trait DiscordIDWithSource {
  def buildDefined(): DiscordIDSourceDefined
}
case object EmptyDiscordID extends DiscordIDWithSource {
  override def buildDefined(): DiscordIDSourceDefined =
    throw new NotImplementedError("cant be called")
}
case class ByHistoryDiscordID(discordID: String) extends DiscordIDWithSource {
  override def buildDefined(): DiscordIDSourceDefined =
    DiscordIDSourceDefined.buildByHistory(discordID)
}
case class ByLogicDiscordID(discordID: String) extends DiscordIDWithSource {
  override def buildDefined(): DiscordIDSourceDefined =
    DiscordIDSourceDefined.buildByLogic(discordID)
}

object DiscordIDSource {
  implicit val rw: RW[DiscordIDSource] = macroRW
  def buildByHistory(): DiscordIDSource = DiscordIDSource(Right(None))
  def buildByHistory(smurf: String): DiscordIDSource =
    DiscordIDSource(Right(Some(smurf)))
  def buildByLogic(smurf: String): DiscordIDSource =
    DiscordIDSource(Left(Some(smurf)))

}
case class ChallongePlayer(
    discordID: Either[String, DiscordIDSource],
    player: SCPlayer
)

object ChallongePlayer {
  implicit val rw: RW[ChallongePlayer] = macroRW
}

case class ChallongeOneVsOneMatchGameResult(
    winner: ChallongePlayer,
    loser: ChallongePlayer
)
object ChallongeOneVsOneMatchGameResult {
  implicit val rw: RW[ChallongeOneVsOneMatchGameResult] = macroRW
}
