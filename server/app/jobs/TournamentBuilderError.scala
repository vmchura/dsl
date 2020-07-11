package jobs


sealed trait TournamentBuilderError extends Exception
case class TournamentAlreadyCreated(challongeTournamentID: String) extends TournamentBuilderError
case class CannontAccessChallongeTournament(challongID: String) extends TournamentBuilderError
case class CannotAccesDiscordGuild(discordGuild: String) extends TournamentBuilderError
case class CannotAddSomeParticipant(participantError: String) extends TournamentBuilderError
case class UnknowError(error: String) extends TournamentBuilderError