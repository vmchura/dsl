package controllers
import database.TemporalDB
import play.api.test.Helpers._
import database.DataBaseObjects._
import models.daos.UserSmurfDAO
import models.{DiscordID, Smurf}
import org.scalatest._
import org.scalatest.compatible.Assertion
class ReplayMatchControllerPushReplayTest extends TemporalDB {
  def validateSmurfs(
      discordID: DiscordID,
      smurfsChecked: Seq[String],
      smurfsOnNotChecked: Seq[String]
  ): Assertion = {
    val userSmurfDAO: UserSmurfDAO =
      app.injector.instanceOf(classOf[UserSmurfDAO])
    val userSmurf = userSmurfDAO.findUser(discordID.id).futureValue
    userSmurf.fold(Assertions.fail("user not found")) { user =>
      {
        user.matchSmurf.map(
          _.smurf
        ) must contain theSameElementsAs smurfsChecked
        user.notCheckedSmurf.map(
          _.smurf
        ) must contain theSameElementsAs smurfsOnNotChecked
      }
    }

  }
  "Push replay" should {
    "NOt be inserted if not proper user" in {
      addSmurfToUser(third_user, Smurf("G19"))
      initTournament()
      val result = ControllersUtil
        .addReplayToMatch(app)(
          "/G19Vs.Chester.rep",
          first_user,
          second_user,
          first_match.matchPK.challongeMatchID,
          Some("G19")
        )

      status(result) mustEqual SEE_OTHER

      result.futureValue.newFlash
        .map(_.data)
        .map(_.contains("error")) mustBe Some(true)

    }
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
          Some("G19")
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

    "Insert on querying smurf" in {
      initTournament()
      initUser(first_user)
      initUser(second_user)
      addSmurfToUser(first_user, Smurf("G19"))
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
        List("G19"),
        Nil
      )
      validateSmurfs(
        DiscordID(second_user.loginInfo.providerKey),
        Nil,
        List(".Chester")
      )
    }

    "Insert on rival smurf" in {
      initTournament()
      initUser(first_user)
      initUser(second_user)
      addSmurfToUser(second_user, Smurf(".Chester"))
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
