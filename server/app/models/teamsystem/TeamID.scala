package models.teamsystem

import models.DiscordID
import play.api.libs.json.{JsObject, JsResult, JsString, JsValue, Json, OFormat}

import java.util.UUID
import scala.util.Try

case class TeamID(id: UUID) extends AnyVal
object TeamID {
  implicit val jsonFormat: OFormat[TeamID] =
    Json.format[TeamID]
}
sealed trait MemberStatus {
  def name: String
}
case object Official extends MemberStatus {
  override def name: String = "Official"
}
case object Suplente extends MemberStatus {
  override def name: String = "Suplente"
}
object MemberStatus {
  def apply(statusString: String): MemberStatus = {
    if (statusString.equals(Official.name)) {
      Official
    } else {
      if (statusString.equals(Suplente.name)) {
        Suplente
      } else {
        throw new IllegalArgumentException(
          s"$statusString is not a valid status name"
        )
      }
    }
  }
  implicit val jsonFormat: OFormat[MemberStatus] = new OFormat[MemberStatus] {
    override def reads(json: JsValue): JsResult[MemberStatus] =
      JsResult.fromTry(Try(MemberStatus((json \ "status").as[String])))

    override def writes(o: MemberStatus): JsObject =
      JsObject(Map("status" -> JsString(o.toString)))
  }

}
case class Member(userID: DiscordID, memberStatus: MemberStatus)
object Member {
  implicit val jsonFormat: OFormat[Member] =
    Json.format[Member]
}
case class Team(
    teamID: TeamID,
    teamName: String,
    principal: DiscordID,
    members: Seq[Member]
) {

  def canBeAdded(userID: DiscordID): Boolean = !isMember(userID)
  def canBeAddedAsSuplente(userID: DiscordID): Boolean =
    canBeAdded(userID) && !members.exists(_.memberStatus == Suplente)
  def isOfficial(userID: DiscordID): Boolean =
    members.exists(m => m.userID == userID && m.memberStatus == Official)
  def isMember(userID: DiscordID): Boolean = members.exists(_.userID == userID)
}
object Team {
  implicit val jsonFormat: OFormat[Team] =
    Json.format[Team]
}
