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
  def resultParseReplay(
      app: play.api.Application
  )(resourceReplay: String, userQuerying: User, rivalUser: User)(implicit
      env: Environment[DefaultEnv]
  ): Future[Result] = {
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
    val multipartFormData =
      MultipartFormData[TemporaryFile](
        dataParts = Map.empty,
        files = Seq(part),
        badParts = Nil
      )

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
}
