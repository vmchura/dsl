package jobs

sealed trait ReplayPusherError extends JobError
case class TournamentNotFoundToReplay(tournamentID: Long) extends ReplayPusherError
case class TournamentNotFoundOnChallonge(tournamentID: String) extends ReplayPusherError
case class MatchNotFoundOnChallonge(challongeID: Long) extends ReplayPusherError
case class UnknowReplayPusherError(error: String) extends ReplayPusherError
object CannotSaveResultMatch extends ReplayPusherError
object CannotInsertOnDropBox extends ReplayPusherError
object CannotSmurf extends ReplayPusherError
