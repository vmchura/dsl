package teamsystem

import backendprotocol.{JavaScriptRoutes, PlayAjax}
import com.thoughtworks.binding.Binding.Var
import com.thoughtworks.binding.{Binding, FutureBinding}
import org.lrng.binding.html
import org.scalajs.dom.raw.{
  File,
  FormData,
  HTMLButtonElement,
  HTMLTableRowElement
}
import shared.models.ReplayTeamID
import shared.models.teamsystem.{
  ReplaySaved,
  SmurfToVerify,
  SpecificTeamReplayResponse,
  TeamReplayError,
  TeamReplayResponse
}
import upickle.default._

import scala.concurrent.Future
import scala.util.{Failure, Success}
import scala.xml.Elem
import scalajs.concurrent.JSExecutionContext.Implicits.queue
class UploadTeamReplayComponent(file: File) {

  implicit def elem2TableRow(e: Elem): Binding[HTMLTableRowElement] =
    throw new NotImplementedError()
  implicit def elem2Button(e: Elem): Binding[HTMLButtonElement] =
    throw new NotImplementedError()
  private val submitReplay =
    JavaScriptRoutes.controllers.teamsystem.TeamReplayController
      .submitTeamReplay()
  val playAjax = new PlayAjax(submitReplay)
  val data = new FormData()
  data.append("replay_file", file)

  private val futVar = Var[Future[Either[String, TeamReplayResponse]]](
    playAjax
      .callByAjaxWithTextParser(
        text => read[TeamReplayResponse](text),
        data
      )
  )

  @html
  private def buildButtonAccept(smurf: String, replayTeamID: ReplayTeamID) =
    Binding {
      val button: Binding[HTMLButtonElement] = <button>{smurf}</button>
      button.bind.onclick = _ => {
        val playAjaxSmurf = new PlayAjax(
          JavaScriptRoutes.controllers.teamsystem.TeamReplayController
            .selectSmurf(smurf, replayTeamID.id)
        )
        futVar.value = playAjaxSmurf.callByAjaxWithTextParser(text =>
          read[TeamReplayResponse](text)
        )
      }
      button.bind
    }

  @html
  private val content = Binding {
    FutureBinding(futVar.bind).bind match {
      case Some(Success(Left(error))) => <p> {error}</p>
      case Some(Success(Right(teamReplayResponse))) =>
        SpecificTeamReplayResponse(teamReplayResponse) match {
          case Some(SmurfToVerify(teamReplayID, oneVsOne)) =>
            <div>
              <p>¿Con qué smurf/nick/nombre jugaste?</p>
              {buildButtonAccept(oneVsOne.winner.smurf, teamReplayID)}
              {buildButtonAccept(oneVsOne.loser.smurf, teamReplayID)}
            </div>
          case Some(TeamReplayError(reason)) => <p> {reason}</p>
          case Some(ReplaySaved()) =>
            <p>Replay guardada satisfactoriamente</p>
          case None => <p> Error en la respuesta del servidor</p>
        }
      case Some(Failure(error)) => <p> {error.getMessage}</p>
      case None                 => <div class="spinner-border" >
          <span class="visually-hidden">Processing...</span>
        </div>
    }

  }

  @html
  private val rowClass = Binding {
    FutureBinding(futVar.bind).bind match {
      case Some(Success(Left(_))) => "table-danger"
      case Some(Success(Right(teamReplayResponse))) =>
        SpecificTeamReplayResponse(teamReplayResponse) match {
          case Some(SmurfToVerify(_, _)) => "table-info"
          case Some(TeamReplayError(_))  => "table-danger"
          case Some(ReplaySaved())       => "table-success"
          case None                      => "table-danger"
        }
      case Some(Failure(_)) => "table-danger"
      case None             => "table-light"
    }
  }

  @html
  def render(): Binding[HTMLTableRowElement] = {
    <tr class={rowClass.bind}>
      <td> {file.name} </td>
      <td> {content.bind} </td>
    </tr>
  }
}
