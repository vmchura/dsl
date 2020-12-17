package controllers

import org.specs2.execute.FailureException
import org.specs2.matcher.JUnitMustMatchers.failure
import play.api.libs.Files.{TemporaryFile, TemporaryFileCreator}
import play.api.mvc.{MultipartFormData, Result}
import play.api.mvc.MultipartFormData.FilePart
import play.api.test.CSRFTokenHelper.addCSRFToken
import play.api.test.FakeRequest
import play.api.test.Helpers._
import com.mohiva.play.silhouette.test._
import utils.auth.DefaultEnv
import com.mohiva.play.silhouette.api.Environment
import models.User

import java.io.File
import scala.concurrent.Future
object ControllersUtil {
  private def buildMultiFormData(
      app: play.api.Application
  )(
      resourceReplay: String,
      dataParts: Map[String, Seq[String]] = Map.empty
  ): MultipartFormData[TemporaryFile] = {
    val fileSource = new File(
      getClass.getResource(resourceReplay).getPath
    )
    val fileResult = File.createTempFile("replay", ".rep")
    import java.nio.file.StandardCopyOption.REPLACE_EXISTING
    val tmpFileCreator =
      app.injector.instanceOf(classOf[TemporaryFileCreator])
    java.nio.file.Files
      .copy(fileSource.toPath, fileResult.toPath, REPLACE_EXISTING)
    val file =
      tmpFileCreator.create(
        fileResult.toPath
      )

    val part = FilePart[TemporaryFile](
      key = "replay_file",
      filename = "replay.rep",
      contentType = None,
      ref = file
    )

    MultipartFormData[TemporaryFile](
      dataParts = dataParts,
      files = Seq(part),
      badParts = Nil
    )
  }
  def resultParseReplay(
      app: play.api.Application
  )(
      resourceReplay: String,
      userQuerying: User,
      rivalUser: User
  )(implicit
      env: Environment[DefaultEnv]
  ): Future[Result] = {

    val multipartFormData = buildMultiFormData(app)(resourceReplay, Map.empty)
    route(
      app,
      addCSRFToken(
        FakeRequest(
          routes.ReplayMatchController.parseReplay(
            userQuerying.loginInfo.providerKey,
            rivalUser.loginInfo.providerKey
          )
        ).withAuthenticator[DefaultEnv](userQuerying.loginInfo)
          .withMultipartFormDataBody(multipartFormData)
      )
    ).getOrElse(throw FailureException(failure("required Some")))
  }

  def addReplayToMatch(
      app: play.api.Application
  )(
      resourceReplay: String,
      userQuerying: User,
      rivalUser: User,
      matchID: Long,
      smurfOfUserQuerying: Option[String] = None
  )(implicit
      env: Environment[DefaultEnv]
  ): Future[Result] = {
    import database.DataBaseObjects._
    val multipartFormData = buildMultiFormData(app)(
      resourceReplay,
      smurfOfUserQuerying.fold(Map.empty[String, Seq[String]])(smurf =>
        Map("mySmurf" -> Seq(smurf))
      )
    )
    route(
      app,
      addCSRFToken(
        FakeRequest(
          routes.ReplayMatchController.addReplayToMatch(
            tournamentID = tournamentTest.challongeID,
            matchID,
            userQuerying.loginInfo.providerKey,
            rivalUser.loginInfo.providerKey
          )
        ).withAuthenticator[DefaultEnv](userQuerying.loginInfo)
          .withMultipartFormDataBody(multipartFormData)
      )
    ).getOrElse(throw FailureException(failure("required Some")))
  }
}
