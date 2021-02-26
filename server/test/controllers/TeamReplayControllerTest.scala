package controllers
import play.api.test.Helpers._
import akka.http.scaladsl.model.HttpHeader.ParsingResult.Ok
import database.DataBaseObjects.{
  first_match,
  first_user,
  second_user,
  third_user
}
import database.TemporalDB
import models.Smurf
import models.daos.DiscordPlayerLoggedDAO
import models.daos.teamsystem.{TeamDAO, TeamUserSmurfPendingDAO}
import models.teamsystem.{Member, MemberStatus}
import org.scalatestplus.play.PlaySpec
import play.api.test.CSRFTokenHelper.addCSRFToken
import play.api.test.FakeRequest
import play.api.test.Helpers.status
import shared.models.{DiscordID, DiscordPlayerLogged}
import utils.auth.DefaultEnv

import scala.concurrent.ExecutionContext.Implicits.global
import com.mohiva.play.silhouette.test._

import java.lang.IllegalStateException
import java.util.UUID
class TeamReplayControllerTest
    extends PlaySpec
    with TemporalDB
    with UtilReplayMatchController {
  def createTeamAndAdd(owner: DiscordID): Unit = {
    implicit val teamDAO: TeamDAO = app.injector.instanceOf(classOf[TeamDAO])
    val _ = (for {
      created <- teamDAO.save(owner, "skt1")
      added <- teamDAO.addMemberTo(
        Member(owner, MemberStatus.Official),
        created
      )
    } yield {
      added
    }).futureValue
  }
  "Push replay" should {
    "Don't allow if not member of team" in {
      val result = ControllersUtil
        .resultParseTeamReplay(app)(
          "/G19Vs.Chester.rep",
          first_user
        )
      status(result) mustEqual OK
      val bodyText = contentAsString(result)

      assert(bodyText.contains("You are not official member of a team"))
    }
    "Response with select smurfs" in {
      createTeamAndAdd(DiscordID(first_user.loginInfo.providerKey))
      val result = ControllersUtil
        .resultParseTeamReplay(app)(
          "/G19Vs.Chester.rep",
          first_user
        )
      status(result) mustEqual OK
      val bodyText = contentAsString(result)

      assert(bodyText.contains("needs confirmation"))
    }
    "Response with smurf to be checked" in {
      createTeamAndAdd(DiscordID(first_user.loginInfo.providerKey))
      addSmurfToUser(third_user, Smurf("G19"))
      import eu.timepit.refined.auto._
      val discordLogged: DiscordPlayerLoggedDAO =
        app.injector.instanceOf(classOf[DiscordPlayerLoggedDAO])
      discordLogged
        .add(
          DiscordPlayerLogged(
            DiscordID(third_user.loginInfo.providerKey),
            "Gustavo",
            "0000",
            None
          )
        )
        .futureValue
      val result = ControllersUtil
        .resultParseTeamReplay(app)(
          "/G19Vs.Chester.rep",
          first_user
        )
      status(result) mustEqual OK
      val bodyText = contentAsString(result)
      assert(bodyText.contains("replay saved"))
      val pending = app.injector.instanceOf(classOf[TeamUserSmurfPendingDAO])
      whenReady(pending.load(DiscordID(first_user.loginInfo.providerKey))) {
        res =>
          assertResult(1)(res.length)
          assertResult(Some(Smurf(".Chester")))(res.headOption.map(_.smurf))
      }
    }
    "Response with smurfs taken" in {
      createTeamAndAdd(DiscordID(first_user.loginInfo.providerKey))
      addSmurfToUser(second_user, Smurf(".Chester"))
      addSmurfToUser(third_user, Smurf("G19"))
      import eu.timepit.refined.auto._
      val discordLogged: DiscordPlayerLoggedDAO =
        app.injector.instanceOf(classOf[DiscordPlayerLoggedDAO])
      discordLogged
        .add(
          DiscordPlayerLogged(
            DiscordID(second_user.loginInfo.providerKey),
            "Chester",
            "0000",
            None
          )
        )
        .futureValue
      discordLogged
        .add(
          DiscordPlayerLogged(
            DiscordID(third_user.loginInfo.providerKey),
            "Gustavo",
            "0001",
            None
          )
        )
        .futureValue
      val result = ControllersUtil
        .resultParseTeamReplay(app)(
          "/G19Vs.Chester.rep",
          first_user
        )
      status(result) mustEqual OK
      val bodyText = contentAsString(result)
      assert(bodyText.contains("otros"))
      val pending = app.injector.instanceOf(classOf[TeamUserSmurfPendingDAO])
      whenReady(pending.load(DiscordID(first_user.loginInfo.providerKey))) {
        res =>
          assertResult(0)(res.length)
      }
    }
    "Response with smurf to be checked once smurf selected" in {
      createTeamAndAdd(DiscordID(first_user.loginInfo.providerKey))
      val result = ControllersUtil
        .resultParseTeamReplay(app)(
          "/G19Vs.Chester.rep",
          first_user
        )
      status(result) mustEqual OK
      val bodyText = contentAsString(result)
      val initIndex = bodyText.indexOf('[')
      val endIndex = bodyText.indexOf(']')
      val replayTeamID =
        UUID.fromString(bodyText.substring(initIndex + 1, endIndex))
      val finalResult = route(
        app,
        addCSRFToken(
          FakeRequest(
            controllers.teamsystem.routes.TeamReplayController
              .selectSmurf("G19", replayTeamID)
          ).withAuthenticator[DefaultEnv](first_user.loginInfo)
        )
      ).getOrElse(throw new IllegalStateException("Select smurf broken"))

      val finalBodyText = contentAsString(finalResult)
      assert(finalBodyText.contains("replay saved"))
      val pending = app.injector.instanceOf(classOf[TeamUserSmurfPendingDAO])
      whenReady(pending.load(DiscordID(first_user.loginInfo.providerKey))) {
        res =>
          assertResult(1)(res.length)
          assertResult(Some(Smurf("G19")))(res.headOption.map(_.smurf))
      }
      assert(bodyText.contains("needs confirmation"))
    }
  }
}
