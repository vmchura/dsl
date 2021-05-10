package modules

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.util.HTTPLayer
import com.mohiva.play.silhouette.impl.exceptions.ProfileRetrievalException
import com.mohiva.play.silhouette.impl.providers._
import TrovoProvider._
import play.api.libs.json.{JsObject, JsValue}
import play.api.mvc.RequestHeader

import scala.concurrent.Future

trait BaseTrovoProvider extends OAuth2Provider {

  override type Content = JsValue

  override val id: String = ID

  override protected val urls = Map("api" -> settings.apiURL.getOrElse(API))

  override protected def buildProfile(authInfo: OAuth2Info): Future[Profile] = {

    httpLayer
      .url(
        urls("api").format(
          authInfo.accessToken,
          controllers.routes.TrovoController.trovoCallBack("", 1, "").url
        )
      )
      .get()
      .flatMap { response =>
        val json = response.json

        (json \ "error").asOpt[JsObject] match {
          case Some(error) =>
            val errorMsg = (error \ "message").as[String]
            val errorType = (error \ "type").as[String]
            val errorCode = (error \ "code").as[Int]
            throw new ProfileRetrievalException(
              SpecifiedProfileError.format(id, errorMsg, errorType, errorCode)
            )
          case _ => profileParser.parse(json, authInfo)
        }

      }
  }
}

class TrovoProfileParser
    extends SocialProfileParser[JsValue, CommonSocialProfile, OAuth2Info] {

  override def parse(
      json: JsValue,
      authInfo: OAuth2Info
  ): Future[CommonSocialProfile] = {
    Future.successful {
      val userID = (json \ "id").as[String]
      val fullName = (json \ "username").asOpt[String]
      val avatarURL = (json \ "avatar")
        .asOpt[String]
        .map(avatarhash =>
          s"https://cdn.Trovoapp.com/avatars/$userID/$avatarhash.png"
        )

      CommonSocialProfile(
        loginInfo = LoginInfo(ID, userID),
        firstName = None,
        lastName = None,
        fullName = fullName,
        avatarURL = avatarURL,
        email = None
      )
    }
  }
}

class TrovoProvider(
    protected val httpLayer: HTTPLayer,
    protected val stateHandler: SocialStateHandler,
    val settings: OAuth2Settings
) extends BaseTrovoProvider
    with CommonSocialProfileBuilder {

  override type Self = TrovoProvider

  override val profileParser = new TrovoProfileParser

  override def withSettings(
      f: OAuth2Settings => OAuth2Settings
  ): TrovoProvider =
    new TrovoProvider(httpLayer, stateHandler, f(settings))
}

object TrovoProvider {
  val SpecifiedProfileError =
    "[Silhouette][%s] Error retrieving profile information. Error message: %s, type: %s, code: %s"

  val ID = "trovo"
  //    Redirect(s"https://open.trovo.live/page/login.html?client_id=$client_id&response_type=token&scope=user_details_self&redirect_uri=$redirect_uri&state=$randomString",302)
  val API =
    "https://open.trovo.live/page/login.html" +
      "?client_id=%s" +
      "&response_type=token" +
      "&scope=user_details_self+channel_subscriptions" +
      "&redirect_uri=%s" +
      "&state=%s"
}
