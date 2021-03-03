package backendprotocol

import shared.PlayCall
import shared.models.teamsystem.TeamReplayResponse
import shared.models.{ChallongeOneVsOneMatchGameResult, DiscordPlayerLogged}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobal

@js.native
@JSGlobal("jsRoutes")
object JavaScriptRoutes extends js.Object {
  def controllers: Tcontrollers = js.native
}
@js.native
trait Tcontrollers extends js.Object {
  val ReplayMatchController: TReplayMatchController = js.native
  val teamsystem: TTeamSystem = js.native
}

@js.native
trait TReplayMatchController extends js.Object {
  def parseReplay(
      discordUser1: String,
      discordUser2: String
  ): PlayCall[Either[String, ChallongeOneVsOneMatchGameResult]] = js.native
}
@js.native
trait TTeamSystem extends js.Object {
  val MemberSupervisorController: TMemberSupervisorController = js.native
  val TeamReplayController: TTeamReplayController = js.native
}

@js.native
trait TMemberSupervisorController extends js.Object {
  def findMembers(): PlayCall[Either[String, Seq[DiscordPlayerLogged]]] =
    js.native
}

@js.native
trait TTeamReplayController extends js.Object {
  def submitTeamReplay(): PlayCall[TeamReplayResponse] =
    js.native
}
