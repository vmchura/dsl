package controllers
import play.api.libs.Files.{TemporaryFile, TemporaryFileCreator}

import java.io.File
import play.api.mvc.MultipartFormData
import play.api.mvc.MultipartFormData.FilePart
import database.TemporalDB

import play.api.test.CSRFTokenHelper._
import com.mohiva.play.silhouette.test._
import play.api.test._
import shared.models.{
  ChallongeOneVsOneMatchGameResult,
  ChallongePlayer,
  DiscordIDSource
}
import upickle.default._
import database.DataBaseObjects._
import org.specs2.execute.FailureException
import org.specs2.matcher.JUnitMustMatchers.failure
import play.api.test.Helpers._
import shared.models.StarCraftModels.{Protoss, SCPlayer}
import utils.auth.DefaultEnv
class ReplayMatchControllerTest extends TemporalDB {
  "The `user` method" should {
    "return status 401 if no authenticator was found" in {
      val tmpFileCreator =
        app.injector.instanceOf(classOf[TemporaryFileCreator])
      val file =
        tmpFileCreator.create(
          new File(
            getClass.getResource("/G19Vs.Chester.rep").getPath
          ).toPath
        )
      val part = FilePart[TemporaryFile](
        key = "replay_file",
        filename = "replay.rep",
        contentType = None,
        ref = file
      )
      val multipartFormData =
        MultipartFormData[TemporaryFile](
          dataParts = Map.empty,
          files = Seq(part),
          badParts = Nil
        )

      val result = route(
        app,
        addCSRFToken(
          FakeRequest(
            routes.ReplayMatchController.parseReplay(
              first_user.loginInfo.providerKey,
              second_user.loginInfo.providerKey
            )
          ).withAuthenticator[DefaultEnv](first_user.loginInfo)
            .withMultipartFormDataBody(multipartFormData)
        )
      ).getOrElse(throw FailureException(failure("required Some")))

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
  }
}
