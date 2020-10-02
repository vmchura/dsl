package models
import org.joda.time.DateTime
import org.scalatestplus.play.PlaySpec
import play.api.libs.json._
import eu.timepit.refined.auto._
import scala.util.Random
class DiscordUserLogTest extends PlaySpec  {
  val guildID = GuildID(Random.nextString(12))
  "save DateTime in properJson" in {

    val u = DiscordUserLog(Random.nextString(12),guildID,None,DateTime.now())
    val js = Json.toJson(u)
    val userLogRecoverd: JsResult[DiscordUserLog] = Json.fromJson[DiscordUserLog](js)
    assertResult(JsSuccess(u).map(_.date.getMillis))(userLogRecoverd.map(_.date.getMillis))
    assertResult(JsSuccess(u).map(_.userName))(userLogRecoverd.map(_.userName))

  }
  "save and recovery DiscordUserHistory" in {
    val log = DiscordUserLog(Random.nextString(12),guildID,None,DateTime.now())
    val userHistory = DiscordUserHistory(DiscordID(Random.nextString(12)),"1343",Random.nextString(12),Seq(log))
    val userHistoryRecovered = Json.fromJson[DiscordUserHistory](Json.toJson(userHistory))
    val mapper: DiscordUserHistory => String = _.discriminator
    assertResult(JsSuccess(userHistory).map(mapper))(userHistoryRecovered.map(mapper))
  }
}
