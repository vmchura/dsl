package modules

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.util.HTTPLayer
import com.mohiva.play.silhouette.impl.exceptions.ProfileRetrievalException
import com.mohiva.play.silhouette.impl.providers._
import DiscordProvider._
import play.api.libs.json.{ JsObject, JsValue }
import scala.concurrent.Future

trait BaseDiscordProvider extends OAuth2Provider {

  override type Content = JsValue

  override val id: String = ID

  override protected val urls = Map("api" -> settings.apiURL.getOrElse(API))

  override protected def buildProfile(authInfo: OAuth2Info): Future[Profile] = {
    httpLayer.url(urls("api").format(authInfo.accessToken)).get().flatMap { response =>
      val json = response.json
      (json \ "error").asOpt[JsObject] match {
        case Some(error) =>
          val errorMsg = (error \ "message").as[String]
          val errorType = (error \ "type").as[String]
          val errorCode = (error \ "code").as[Int]
          throw new ProfileRetrievalException(SpecifiedProfileError.format(id, errorMsg, errorType, errorCode))
        case _ => profileParser.parse(json, authInfo)
      }

    }
  }
}

class DiscordProfileParser extends SocialProfileParser[JsValue, CommonSocialProfile, OAuth2Info] {

  override def parse(json: JsValue, authInfo: OAuth2Info): Future[CommonSocialProfile] = Future.successful{
    val userID = (json \ "id").as[String]
    val firstName = (json \ "first_name").asOpt[String]
    val lastName = (json \ "last_name").asOpt[String]
    val fullName = (json \ "name").asOpt[String]
    val avatarURL = (json \ "picture" \ "data" \ "url").asOpt[String]
    val email = (json \ "email").asOpt[String]

    CommonSocialProfile(
      loginInfo = LoginInfo(ID, userID),
      firstName = firstName,
      lastName = lastName,
      fullName = fullName,
      avatarURL = avatarURL,
      email = email)
  }
}


class DiscordProvider(
                       protected val httpLayer: HTTPLayer,
                       protected val stateHandler: SocialStateHandler,
                       val settings: OAuth2Settings)
  extends BaseDiscordProvider with CommonSocialProfileBuilder {

  override type Self = DiscordProvider

  override val profileParser = new DiscordProfileParser

  override def withSettings(f: OAuth2Settings => OAuth2Settings): DiscordProvider = new DiscordProvider(httpLayer, stateHandler, f(settings))
}

object DiscordProvider{
  val SpecifiedProfileError = "[Silhouette][%s] Error retrieving profile information. Error message: %s, type: %s, code: %s"

  val ID = "discord"
  val API = "https://discord.com/api/v6"
}