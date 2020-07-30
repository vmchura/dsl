package models

case class MatchDiscord(matchPK: MatchPK,round: String, firstChaNameID: Long,
                        secondChaNameID: Long,discord1ID: String, discord2ID: String,
                        player1Name: String, player2Name: String, replaysAttached: Seq[ReplayRecord] = Nil) extends TWithReplays [MatchDiscord]{
  override def withReplays(replays: Seq[ReplayRecord]): MatchDiscord = copy(replaysAttached = replays)

  def toMatch(): Match = Match(matchPK, firstChaNameID, secondChaNameID, round, Some(player1Name), Some(player2Name), replaysAttached)
}