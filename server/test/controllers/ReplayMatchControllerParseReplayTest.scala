package controllers
import database.TemporalDB
import shared.models.{
  ChallongeOneVsOneMatchGameResult,
  ChallongePlayer,
  DiscordIDSource
}
import upickle.default._
import play.api.test.Helpers._
import shared.models.StarCraftModels.{Protoss, SCPlayer}
import database.DataBaseObjects._
import models.Smurf
import org.scalatestplus.play.PlaySpec
class ReplayMatchControllerParseReplayTest extends PlaySpec with TemporalDB {
  "Parse replay" should {
    "return (empty, empty) smurfs" in {

      val result = ControllersUtil
        .resultParseReplay(app)("/G19Vs.Chester.rep", first_user, second_user)
      status(result) mustEqual OK
      val value = read[Either[String, ChallongeOneVsOneMatchGameResult]](
        (contentAsJson(result) \ "response").as[String]
      )
      assertResult(
        Right(
          ChallongeOneVsOneMatchGameResult(
            ChallongePlayer(
              Right(DiscordIDSource.buildByHistory()),
              SCPlayer("G19", Protoss)
            ),
            ChallongePlayer(
              Right(DiscordIDSource.buildByHistory()),
              SCPlayer(".Chester", Protoss)
            )
          )
        )
      )(value)

    }
    "return (some,empty) smurfs" in {

      addSmurfToUser(first_user, Smurf("G19"))
      val result = ControllersUtil
        .resultParseReplay(app)("/G19Vs.Chester.rep", first_user, second_user)
      status(result) mustEqual OK
      val value = read[Either[String, ChallongeOneVsOneMatchGameResult]](
        (contentAsJson(result) \ "response").as[String]
      )
      assertResult(
        Right(
          ChallongeOneVsOneMatchGameResult(
            ChallongePlayer(
              Right(
                DiscordIDSource.buildByHistory(first_user.loginInfo.providerKey)
              ),
              SCPlayer("G19", Protoss)
            ),
            ChallongePlayer(
              Right(
                DiscordIDSource.buildByLogic(second_user.loginInfo.providerKey)
              ),
              SCPlayer(".Chester", Protoss)
            )
          )
        )
      )(value)

    }
    "return (some,some) smurfs" in {

      addSmurfToUser(first_user, Smurf("G19"))
      addSmurfToUser(second_user, Smurf(".Chester"))
      val result = ControllersUtil
        .resultParseReplay(app)("/G19Vs.Chester.rep", first_user, second_user)
      status(result) mustEqual OK
      val value = read[Either[String, ChallongeOneVsOneMatchGameResult]](
        (contentAsJson(result) \ "response").as[String]
      )
      assertResult(
        Right(
          ChallongeOneVsOneMatchGameResult(
            ChallongePlayer(
              Right(
                DiscordIDSource.buildByHistory(first_user.loginInfo.providerKey)
              ),
              SCPlayer("G19", Protoss)
            ),
            ChallongePlayer(
              Right(
                DiscordIDSource.buildByHistory(
                  second_user.loginInfo.providerKey
                )
              ),
              SCPlayer(".Chester", Protoss)
            )
          )
        )
      )(value)

    }
    "return (error,error) smurfs" in {

      addSmurfToUser(first_user, Smurf("G19"))
      addSmurfToUser(third_user, Smurf(".Chester"))
      val result = ControllersUtil
        .resultParseReplay(app)("/G19Vs.Chester.rep", first_user, second_user)
      status(result) mustEqual OK
      val value = read[Either[String, ChallongeOneVsOneMatchGameResult]](
        (contentAsJson(result) \ "response").as[String]
      )
      assertResult(
        Right(
          ChallongeOneVsOneMatchGameResult(
            ChallongePlayer(
              Left("Impossible, can't locate proper user"),
              SCPlayer("G19", Protoss)
            ),
            ChallongePlayer(
              Left("Impossible, can't locate proper user"),
              SCPlayer(".Chester", Protoss)
            )
          )
        )
      )(value)

    }

  }
}
