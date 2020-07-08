package models

case class ChallongeTournament(tournament: Tournament, participants: Seq[Participant], matches: Seq[Match]){
  override def toString: String = {
    s"""$tournament
       |${participants.mkString("\n" +
"      |")}
       |${
      matches.mkString("\n" +
"      |")}
       |""".stripMargin

  }
}
