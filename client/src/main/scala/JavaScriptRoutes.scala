import java.util.UUID
import shared.PlayCall
import shared.models.{
  ActionByReplay,
  ChallongeOneVsOneMatchGameResult,
  ReplayDescriptionShared,
  ReplayRecordShared
}

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
}

@js.native
trait TReplayMatchController extends js.Object {
  def parseReplay(
      discordUser1: String,
      discordUser2: String
  ): PlayCall[Either[String, ChallongeOneVsOneMatchGameResult]] = js.native
}
