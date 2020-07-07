package models

case class ChallongeTournament(tournament: Tournament, participants: Seq[Participant], matchs: Match)
