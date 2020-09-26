package models

case class MatchDiscord(matchPK: MatchPK,tournamentName: String, round: String, firstChaNameID: Long,
                        secondChaNameID: Long, userSmurf1: UserSmurf, userSmurf2: UserSmurf,
                        replaysAttached: Seq[ReplayRecord] = Nil, userLogged: Option[User] = None) extends TWithReplays [MatchDiscord]{

  override def withReplays(replays: Seq[ReplayRecord]): MatchDiscord = copy(replaysAttached = replays)

  def convertToMatch(): Match = Match(matchPK,tournamentName, firstChaNameID, secondChaNameID, round,
    Some(userSmurf1.discordUser.userName), Some(userSmurf2.discordUser.userName), replaysAttached)

  val userLoggedIsFirstPlayer: Boolean = userLogged.exists(_.loginInfo.providerKey.equals(userSmurf1.discordUser.discordID))
}