package controllers

import database.DataBaseObjects._
import database.TemporalDB
import models.Smurf
import org.scalatestplus.play.PlaySpec
import play.api.test.Helpers._
import shared.models.DiscordID

class ReplayMatchControllerPushReplayReversedTest
    extends PlaySpec
    with TemporalDB
    with UtilReplayMatchController {

  "Push replay, querying second" should {

    "Inserte empty smurfs" in {
      initTournament()
      initUser(first_user)
      initUser(second_user)
      val result = ControllersUtil
        .addReplayToMatch(app)(
          "/G19Vs.Chester.rep",
          first_user,
          second_user,
          first_match.matchPK.challongeMatchID,
          Some(".Chester")
        )

      status(result) mustEqual SEE_OTHER

      result.futureValue.newFlash
        .map(_.data)
        .map(_.contains("success")) mustBe Some(true)
      validateSmurfs(
        DiscordID(first_user.loginInfo.providerKey),
        Nil,
        List(".Chester")
      )
      validateSmurfs(
        DiscordID(second_user.loginInfo.providerKey),
        Nil,
        List("G19")
      )
    }

    "Insert on querying smurf" in {
      initTournament()
      initUser(first_user)
      initUser(second_user)
      addSmurfToUser(first_user, Smurf(".Chester"))
      val result = ControllersUtil
        .addReplayToMatch(app)(
          "/G19Vs.Chester.rep",
          first_user,
          second_user,
          first_match.matchPK.challongeMatchID,
          None
        )

      //println(result.futureValue.newFlash)
      status(result) mustEqual SEE_OTHER

      result.futureValue.newFlash
        .map(_.data)
        .map(_.contains("success")) mustBe Some(true)
      validateSmurfs(
        DiscordID(first_user.loginInfo.providerKey),
        List(".Chester"),
        Nil
      )
      validateSmurfs(
        DiscordID(second_user.loginInfo.providerKey),
        Nil,
        List("G19")
      )
    }

    "Insert on rival smurf" in {
      initTournament()
      initUser(first_user)
      initUser(second_user)
      addSmurfToUser(second_user, Smurf("G19"))
      val result = ControllersUtil
        .addReplayToMatch(app)(
          "/G19Vs.Chester.rep",
          first_user,
          second_user,
          first_match.matchPK.challongeMatchID,
          None
        )

      //println(result.futureValue.newFlash)
      status(result) mustEqual SEE_OTHER

      result.futureValue.newFlash
        .map(_.data)
        .map(_.contains("success")) mustBe Some(true)
      validateSmurfs(
        DiscordID(first_user.loginInfo.providerKey),
        Nil,
        List(".Chester")
      )
      validateSmurfs(
        DiscordID(second_user.loginInfo.providerKey),
        List("G19"),
        Nil
      )
    }

    "Not add more same smurfs" in {
      initTournament()
      initUser(first_user)
      initUser(second_user)
      addSmurfToUser(first_user, Smurf(".Chester"))
      addSmurfToUser(second_user, Smurf("G19"))
      val result = ControllersUtil
        .addReplayToMatch(app)(
          "/G19Vs.Chester.rep",
          first_user,
          second_user,
          first_match.matchPK.challongeMatchID,
          None
        )

      //println(result.futureValue.newFlash)
      status(result) mustEqual SEE_OTHER

      result.futureValue.newFlash
        .map(_.data)
        .map(_.contains("success")) mustBe Some(true)
      validateSmurfs(
        DiscordID(first_user.loginInfo.providerKey),
        List(".Chester"),
        Nil
      )
      validateSmurfs(
        DiscordID(second_user.loginInfo.providerKey),
        List("G19"),
        Nil
      )
    }

  }
}
