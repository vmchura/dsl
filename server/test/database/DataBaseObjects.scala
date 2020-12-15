package database

import com.mohiva.play.silhouette.api.LoginInfo
import models.{Match, MatchPK, Participant, ParticipantPK, Tournament, User}

import java.util.UUID

object DataBaseObjects {
  val challongeID: Long = 1234L
  val tournamentTest: Tournament =
    Tournament(challongeID, "-", "-", "TournamentTest", active = true, None)
  private def buildUser(
      discordID: String,
      name: String,
      silhouetteID: UUID
  ): User =
    User(
      silhouetteID,
      LoginInfo("discord", discordID),
      Some(name),
      None
    )
  private val ids = List(
    "0f1aae80-845a-4518-a2bc-d78e7ab80ace",
    "117aa605-ad6e-440a-b1b5-b8cfba830274",
    "99a7818c-3e4c-4583-aa32-2821ed0fed11",
    "2cb294f0-e2bd-4554-b4fb-d56cdd832f8a"
  ).map(UUID.fromString)
  private val discordIDs = List("first", "second", "third", "fourth")
  private val challongeNamesRegistered =
    List("chFirst", "chSecond", "chThird", "chFourth")
  private val discordNamesRegistered =
    List("disFirst", "disSecond", "disThird", "disFourth")
  private val participantChallonge = List(1L, 2L, 3L, 4L)
  val List(first_user, second_user, third_user, fourth_user) =
    (ids, discordIDs, discordNamesRegistered).zipped.map {
      case (silhouetteID, disID, disName) =>
        buildUser(disID, disName, silhouetteID)
    }
  val List(
    first_participant,
    second_participant,
    third_participant,
    fourth_participant
  ) = (ids, discordIDs, participantChallonge).zipped.map {
    case (id, pref, challongeParticipantID) =>
      Participant(
        ParticipantPK(challongeID, challongeParticipantID),
        pref.toUpperCase,
        Some(pref),
        Some(id)
      )
  }

  val List(first_match, second_match) = List(
    Match(
      MatchPK(challongeID, 1L),
      tournamentTest.tournamentName,
      first_participant.participantPK.chaNameID,
      second_participant.participantPK.chaNameID,
      "Round1",
      None,
      None,
      complete = true
    ),
    Match(
      MatchPK(challongeID, 2L),
      tournamentTest.tournamentName,
      third_participant.participantPK.chaNameID,
      fourth_participant.participantPK.chaNameID,
      "Round1",
      None,
      None,
      complete = true
    )
  )

}
