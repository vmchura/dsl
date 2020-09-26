package jobs

sealed trait ReplayPusherError extends JobError
case class TournamentNotFoundToReplay(tournamentID: Long) extends ReplayPusherError
case class TournamentNotFoundOnChallonge(tournamentID: String) extends ReplayPusherError
case class MatchNotFoundOnChallonge(challongeID: Long) extends ReplayPusherError
case class UnknowReplayPusherError(error: String) extends ReplayPusherError {
  override def toString: String = s"[UnknowReplayPusherError($error)]"
}
object CannotSaveResultMatch extends ReplayPusherError
object CannotInsertOnDropBox extends ReplayPusherError
object CannotInsertS3 extends ReplayPusherError
object CannotInsertDiscord extends ReplayPusherError
object CannotSmurf extends ReplayPusherError
object FileIsAlreadyRegistered extends ReplayPusherError

object NotCompleteMatches extends ReplayPusherError
object NoReplaysEnabled extends ReplayPusherError
object TooManyGames extends ReplayPusherError
object CannotWrapIntoFolder extends ReplayPusherError
object BadOrderReplays extends ReplayPusherError
