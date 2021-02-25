package models.teamsystem

import shared.models.{DiscordID, ReplayTeamID}

case class TeamReplayInfo(
    replayID: ReplayTeamID,
    cloudLocation: String,
    senderID: DiscordID
)
