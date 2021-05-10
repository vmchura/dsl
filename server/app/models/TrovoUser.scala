package models

import play.api.libs.json.{Json, OFormat}

case class TrovoUserID(uid: Long) extends AnyVal
object TrovoUserID {
  implicit val jsonFormat: OFormat[TrovoUserID] = Json.format[TrovoUserID]
}

case class TrovoUser(
    discordID: String,
    trovoUserID: TrovoUserID,
    nickname: String
)
object TrovoUser {
  implicit val jsonFormat: OFormat[TrovoUser] = Json.format[TrovoUser]
}
