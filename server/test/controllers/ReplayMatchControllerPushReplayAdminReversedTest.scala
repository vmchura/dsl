package controllers

import database.DataBaseObjects._
import database.TemporalDB
import models.{DiscordID, Smurf}
import play.api.test.Helpers.{status, _}

class ReplayMatchControllerPushReplayAdminReversedTest
    extends TemporalDB
    with UtilReplayMatchController {

  "Push replay by admin reversed" should {

    "Insert empty smurfs" in {
      initTournament()
      initUser(first_user)
      initUser(second_user)
      val result = ControllersUtil
        .addReplayToMatchByAdmin(app)(
          "/G19Vs.Chester.rep",
          first_user,
          second_user,
          first_match.matchPK.challongeMatchID,
          Some(
            Seq(
              first_user.loginInfo.providerKey,
              ".Chester",
              second_user.loginInfo.providerKey,
              "G19"
            )
          )
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

    "Insert on first smurf" in {
      initTournament()
      initUser(first_user)
      initUser(second_user)
      addSmurfToUser(first_user, Smurf(".Chester"))
      val result = ControllersUtil
        .addReplayToMatchByAdmin(app)(
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

    "Insert on second smurf" in {
      initTournament()
      initUser(first_user)
      initUser(second_user)
      addSmurfToUser(second_user, Smurf("G19"))
      val result = ControllersUtil
        .addReplayToMatchByAdmin(app)(
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
        .addReplayToMatchByAdmin(app)(
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
