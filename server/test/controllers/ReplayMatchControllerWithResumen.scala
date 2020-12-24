package controllers

import database.DataBaseObjects.{first_match, first_user, second_user}
import play.api.test.Helpers.status
import database.TemporalDB
import play.api.test.Helpers._
import models.daos.usertrajectory.ReplayRecordResumenDAO
import models.daos.usertrajectory.ReplayRecordResumenDAO.ByPlayer
import org.scalatest.concurrent.PatienceConfiguration.Interval
import org.scalatest.time.{Seconds, Span}
import org.scalatestplus.play.PlaySpec

class ReplayMatchControllerWithResumen
    extends PlaySpec
    with TemporalDB
    with UtilReplayMatchController {
  private val replayRecordResumenDAO =
    app.injector.instanceOf(classOf[ReplayRecordResumenDAO])
  implicit override val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = Span(20, Seconds), interval = Span(1, Seconds))
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
    val loaded = replayRecordResumenDAO
      .load(ByPlayer(first_user.loginInfo.providerKey))
      .futureValue(interval = Interval(Span(5, Seconds)))
    assertResult(1)(loaded.length)
  }
}
