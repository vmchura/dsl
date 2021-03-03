package teamsystem

import backendprotocol.{JavaScriptRoutes, PlayAjax}
import com.thoughtworks.binding.{Binding, FutureBinding}
import com.thoughtworks.binding.Binding.Var
import org.lrng.binding.html
import org.scalajs.dom.raw.{File, FormData, HTMLTableRowElement}
import shared.models.StarCraftModels.OneVsOne
import shared.models.teamsystem.{
  ReplaySaved,
  SmurfToVerify,
  SpecificTeamReplayResponse,
  TeamReplayError,
  TeamReplayResponse
}
import upickle.default._

import scala.util.{Failure, Success}
import scala.xml.Elem
import scalajs.concurrent.JSExecutionContext.Implicits.queue
class UploadTeamReplayComponent(file: File) {

  implicit def elem2TableRow(e: Elem): Binding[HTMLTableRowElement] = ???
  private val messageResult = Var[Option[String]](None)
  private val smurfToVerify = Var[Option[SmurfToVerify]](None)
  val submitReplay =
    JavaScriptRoutes.controllers.teamsystem.TeamReplayController
      .submitTeamReplay()
  val playAjax = new PlayAjax(submitReplay)
  val data = new FormData()
  data.append("replay_file", file)

  val futValue = playAjax
    .callByAjaxWithTextParser(
      text => {
        read[TeamReplayResponse](text)
      },
      data
    )
  /*
  futValue.onComplete {
    case Success(Left(error)) =>
    case Success(Right(teamReplayResponse)) =>
      SpecificTeamReplayResponse(teamReplayResponse) match {
        case Some(SmurfToVerify(_, _))     =>
        case Some(TeamReplayError(reason)) =>
        case Some(ReplaySaved())           =>
        case None                          =>
      }
    case Failure(exception) =>
  }*/

  @html
  val content = Binding {
    FutureBinding(futValue).bind match {
      case Some(Success(Left(error))) => <p> {error}</p>
      case Some(Success(Right(teamReplayResponse))) =>
        SpecificTeamReplayResponse(teamReplayResponse) match {
          case Some(SmurfToVerify(replayTeamID, oneVsOne)) =>
            <div>
              <p>¿Con qué smurf jugaste</p>
              <button>{oneVsOne.winner.smurf}</button>
              <button>{oneVsOne.loser.smurf}</button>
            </div>
          case Some(TeamReplayError(reason)) => <p> {reason}</p>
          case Some(ReplaySaved())           => <p>Replay guardada satisfactoriamente</p>
          case None                          => <p> Error en la respuesta del servidor</p>
        }
      case Some(Failure(error)) => <p> {error.getMessage}</p>
      case None                 => <div class="spinner-border" >
        <span class="visually-hidden">Processing...</span>
      </div>
    }
  }

  @html
  val rowClass = Binding {
    FutureBinding(futValue).bind match {
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
