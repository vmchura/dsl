package models.teamsystem

import models.daos.DiscordPlayerLoggedDAO
import models.DiscordPlayerLogged
import play.api.libs.json.{JsObject, JsResult, JsString, JsValue, Json, OFormat}
import shared.models.DiscordID

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
object MemberStatus {
  case object Official extends MemberStatus {
    override def name: String = "Official"
  }
  case object Suplente extends MemberStatus {
    override def name: String = "Suplente"
  }
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
  import models.ModelsJsonImplicits._
  implicit val jsonFormat: OFormat[Member] =
    Json.format[Member]
}
case class Team(
    teamID: TeamID,
    teamName: String,
    principal: DiscordID,
    members: Seq[Member]
) {
  import MemberStatus._
  def canBeAdded(userID: DiscordID): Boolean = !isMember(userID)
  def canBeAddedAsSuplente(userID: DiscordID): Boolean =
    canBeAdded(userID) && !members.exists(_.memberStatus == Suplente)
  def isOfficial(userID: DiscordID): Boolean =
    members.exists(m => m.userID == userID && m.memberStatus == Official)
  def isMember(userID: DiscordID): Boolean = members.exists(_.userID == userID)
}
object Team {
  import models.ModelsJsonImplicits._

  implicit val jsonFormat: OFormat[Team] =
    Json.format[Team]
}

case class TeamWithUsers(
    teamID: TeamID,
    teamName: String,
    principal: DiscordPlayerLogged,
    officials: Seq[DiscordPlayerLogged],
    suplentes: Seq[DiscordPlayerLogged]
)
object TeamWithUsers {
  import scala.concurrent.Future
  import scala.concurrent.ExecutionContext.Implicits.global
  def apply(team: Team)(implicit
      discordPlayerLoggedDAO: DiscordPlayerLoggedDAO
  ): Future[Option[TeamWithUsers]] = {
    val loader: DiscordID => Future[Option[DiscordPlayerLogged]] =
      discordPlayerLoggedDAO.load
    val principalFut = loader(team.principal)
    val officialsFut = Future.traverse(
      team.members.filter(_.memberStatus == MemberStatus.Official).map(_.userID)
    )(loader)
    val suplentesFut = Future.traverse(
      team.members.filter(_.memberStatus == MemberStatus.Suplente).map(_.userID)
    )(loader)

    def allAreDefined[T](seq: Seq[Option[T]]): Option[Seq[T]] = {
      if (seq.forall(_.isDefined)) {
        Some(seq.flatten)
      } else {
        None
      }
    }

    for {
      principalOpt <- principalFut
      officialsOpt <- officialsFut
      suplentesOpt <- suplentesFut
    } yield {
      for {
        principal <- principalOpt
        officials <- allAreDefined(officialsOpt)
        suplentes <- allAreDefined(suplentesOpt)
      } yield {
        TeamWithUsers(
          team.teamID,
          team.teamName,
          principal,
          officials,
          suplentes
        )
      }
    }
  }
}
