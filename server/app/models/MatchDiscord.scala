package models

case class MatchDiscord(matchPK: MatchPK,round: String, firstChaNameID: Long,
                        secondChaNameID: Long, userSmurf1: UserSmurf, userSmurf2: UserSmurf,
                        replaysAttached: Seq[ReplayRecord] = Nil) extends TWithReplays [MatchDiscord]{
  override def withReplays(replays: Seq[ReplayRecord]): MatchDiscord = copy(replaysAttached = replays)

  def convertToMatch(): Match = Match(matchPK, firstChaNameID, secondChaNameID, round, Some(userSmurf1.discordUser.userName), Some(userSmurf2.discordUser.userName), replaysAttached)
}