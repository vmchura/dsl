package controllers
import play.api.test.Helpers._
import database.DataBaseObjects._

import scala.collection.parallel.CollectionConverters._
import database.TemporalDB
import models.{DiscordID, Smurf}
import org.scalatestplus.play.PlaySpec
import play.api.test.Helpers.status

class ReplayMatchControllerPushReplayAdminTest
    extends PlaySpec
    with TemporalDB
    with UtilReplayMatchController {

  "Push replay by admin" should {
    "NOt be inserted if not proper user" in {
      addSmurfToUser(third_user, Smurf("G19"))
      initTournament()
      val result = ControllersUtil
        .addReplayToMatchByAdmin(app)(
          "/G19Vs.Chester.rep",
          first_user,
          second_user,
          first_match.matchPK.challongeMatchID,
          Some(
            Seq(
              first_user.loginInfo.providerKey,
              "G19",
              second_user.loginInfo.providerKey,
              ".Chester"
            )
          )
        )

      status(result) mustEqual SEE_OTHER

      result.futureValue.newFlash
        .map(_.data)
        .map(_.contains("error")) mustBe Some(true)

    }
    "Not Insert on empty smurfs with no additional data" in {
      initTournament()
      initUser(first_user)
      initUser(second_user)
      val result = ControllersUtil
        .addReplayToMatchByAdmin(app)(
          "/G19Vs.Chester.rep",
          first_user,
          second_user,
          first_match.matchPK.challongeMatchID,
          None
        )

      status(result) mustEqual SEE_OTHER

      result.futureValue.newFlash
        .map(_.data)
        .map(_.contains("error")) mustBe Some(true)
      validateSmurfs(
        DiscordID(first_user.loginInfo.providerKey),
        Nil,
        Nil
      )
      validateSmurfs(
        DiscordID(second_user.loginInfo.providerKey),
        Nil,
        Nil
      )
    }
    "Inserte empty smurfs" in {
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
              "G19",
              second_user.loginInfo.providerKey,
              ".Chester"
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
        List("G19")
      )
      validateSmurfs(
        DiscordID(second_user.loginInfo.providerKey),
        Nil,
        List(".Chester")
      )
    }

    "Insert on first smurf" in {
      initTournament()
      initUser(first_user)
      initUser(second_user)
      addSmurfToUser(first_user, Smurf("G19"))
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
        List("G19"),
        Nil
      )
      validateSmurfs(
        DiscordID(second_user.loginInfo.providerKey),
        Nil,
        List(".Chester")
      )
    }

    "Insert on second smurf" in {
      initTournament()
      initUser(first_user)
      initUser(second_user)
      addSmurfToUser(second_user, Smurf(".Chester"))
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
        List("G19")
      )
      validateSmurfs(
        DiscordID(second_user.loginInfo.providerKey),
        List(".Chester"),
        Nil
      )
    }

    "Not add more same smurfs" in {
      initTournament()
      initUser(first_user)
      initUser(second_user)
      addSmurfToUser(first_user, Smurf("G19"))
      addSmurfToUser(second_user, Smurf(".Chester"))
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
        List("G19"),
        Nil
      )
      validateSmurfs(
        DiscordID(second_user.loginInfo.providerKey),
        List(".Chester"),
        Nil
      )
    }

    "Support intensive upload" in {
      initTournament()
      initUser(first_user)
      initUser(second_user)
      addSmurfToUser(first_user, Smurf("G19"))
      addSmurfToUser(second_user, Smurf(".Chester"))

      def uploadReplay(): Unit = {
        val result = ControllersUtil
          .addReplayToMatchByAdmin(app)(
            "/G19Vs.Chester.rep",
            first_user,
            second_user,
            first_match.matchPK.challongeMatchID,
            None
          )

        whenReady(result) { res =>
          res.newFlash
            .map(_.data)
            .map(_.contains("success")) mustBe Some(true)
        }

      }

      (1 to 10).par.toList.foreach(_ => uploadReplay())

      validateSmurfs(
        DiscordID(first_user.loginInfo.providerKey),
        List("G19"),
        Nil
      )
      validateSmurfs(
        DiscordID(second_user.loginInfo.providerKey),
        List(".Chester"),
        Nil
      )
    }
  }
}
