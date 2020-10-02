package models

import org.joda.time.DateTime
import play.api.libs.json.{JsError, JsObject, JsPath, JsResult, JsSuccess, JsValue, Json, JsonValidationError, KeyPathNode, OFormat}
import be.venneborg.refined.play.RefinedJsonFormats._
case class DiscordUserLog(userName: String, guildID: GuildID, avatarHash: Option[String], date:DateTime)
object DiscordUserLog{
  implicit val dateFormat: OFormat[DateTime] = new OFormat[DateTime] {
    override def writes(o: DateTime): JsObject = Json.obj("iso" -> o.toString)

    override def reads(json: JsValue): JsResult[DateTime] = {
      try{
        val dateTime = DateTime.parse((json \ "iso").as[String])
        JsSuccess[DateTime](dateTime)
      }catch{
        case e: Throwable =>
          val item: (JsPath, Seq[JsonValidationError]) = (JsPath(List(KeyPathNode("iso"))),Seq(JsonValidationError(e.toString)))
          JsError(Seq(item))
      }
    }
  }
  implicit val jsonFormat: OFormat[DiscordUserLog] = Json.format[DiscordUserLog]
}
case class DiscordUserHistory(discordID: DiscordID, discriminator: DiscordDiscriminator, lastUserName: String, logs: Seq[DiscordUserLog])
object DiscordUserHistory{
  implicit val jsonFormat: OFormat[DiscordUserHistory] = Json.format[DiscordUserHistory]
}
