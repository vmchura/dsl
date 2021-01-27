package controllers

import database.TemporalDB
import models.daos.UserSmurfDAO
import org.scalatest.Assertions
import org.scalatest.compatible.Assertion
import org.scalatestplus.play.PlaySpec
import shared.models.DiscordID

trait UtilReplayMatchController { this: PlaySpec with TemporalDB =>
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
        user.matchSmurf
          .map(
            _.smurf
          )
          .distinct must contain theSameElementsAs smurfsChecked
        user.notCheckedSmurf
          .map(
            _.smurf
          )
          .distinct must contain theSameElementsAs smurfsOnNotChecked
      }
    }

  }
}
