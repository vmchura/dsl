package jobs


sealed trait TournamentBuilderError extends JobError
case class TournamentAlreadyCreated(challongeTournamentID: String) extends TournamentBuilderError
case class TournamentNotBuild(tournamentID: Long) extends TournamentBuilderError
case class CannontAccessChallongeTournament(challongID: String) extends TournamentBuilderError
case class CannotAccesDiscordGuild(discordGuild: String) extends TournamentBuilderError
case class CannotAddSomeParticipant(participantError: String) extends TournamentBuilderError
case class UnknowTournamentBuilderError(error: String) extends TournamentBuilderError{
  override def toString: String = s"UnknowTournamentBuilderError($error)"
}
object CannotInsertSomeDiscordUser extends TournamentBuilderError