package models
import java.util.UUID

import com.mohiva.play.silhouette.api.{Identity, LoginInfo}
import play.api.libs.json._

case class User(
                 userID: UUID,
                 loginInfo: LoginInfo,
                 fullName: Option[String],
                 avatarURL: Option[String]) extends Identity

object User {
  implicit val jsonFormat: OFormat[User] = Json.format[User]
}

