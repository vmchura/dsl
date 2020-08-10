import org.lrng.binding.html
import org.scalajs.dom.html.{Button, Div}
import com.thoughtworks.binding.Binding.Var
import com.thoughtworks.binding.Binding
import org.lrng.binding.html.NodeBinding
import org.scalajs.dom.raw.{FormData, HTMLInputElement}

import scala.util.{Failure, Success}
import org.scalajs.dom.{Event, Node, document}
import shared.models.ActionByReplay
import upickle.default.read

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

class ReplayUpdater(fieldDiv: Div, player1: String, player2: String, discord1: String, discord2: String) {


  sealed trait StateSettingResult
  object FileUnselected extends StateSettingResult {
    override def toString: String = "Seleccione el replay de la partida"
  }
  object FileSelectedWrongType extends StateSettingResult {
    override def toString: String = "El archivo seleccionado debe terminar en .rep"
  }
  object FileSelectedNotSmallSyze extends StateSettingResult {
    override def toString: String = "El archivo debe ser menor a 1Mb"
  }
  object FileSelectedOk extends StateSettingResult{
    override def toString: String = "Archivo seleccionado de manera correcta"
  }
  object FileParsedCorrectly extends StateSettingResult{
    override def toString: String = "Archivo leído correctamente"
  }
  case class FileErrorReceivingParse(error: String) extends StateSettingResult{
    override def toString: String = s"Error en la conexión con el servidor: $error"
  }
  object  FileParsedIncorrectly extends StateSettingResult{
    override def toString: String = "El archivo no se pudo interpretar como replay"
  }
  object FileOnProcessToParse extends StateSettingResult{
    override def toString: String = "Esperando el PRE procesamiento del archivo"
  }
  object FileIsNotOne extends StateSettingResult{
    override def toString: String = "Sólo se debe escoger UN archivo"
  }
  object MatchingUsers extends StateSettingResult{
    override def toString: String = "Relacione al usuario con el nick en el juego"
  }
  object ReadyToSend extends StateSettingResult{
    override def toString: String = ":) Listo para subir el archivo al servidor"
  }
  case class ErrorByServerParsing(message: String) extends StateSettingResult {
    override def toString: String = s"ERROR en el servidor: $message"
  }

  case class ErrorImpossibleMessage(smurf1: Option[String], smurf2: Option[String]) extends StateSettingResult{
    override def toString: String = (smurf1,smurf2) match {
      case (Some(x), Some(y)) => s"El smurf $x o $y ya está asignado a otro usuario."
      case (None, _) => s"El replay no se pudo interpretar correctamente"
      case (_, None) => s"El replay no se pudo interpretar correctamente"
    }
  }

  private val stateUploadProcess = Var[StateSettingResult](FileUnselected)
  private val replayParsed = Var[Option[ActionByReplay]](None)


  @html
  private val messageState = Binding{stateUploadProcess.bind.toString}

  @html
  private val inputFile = {
    val input: HTMLInputElement = org.scalajs.dom.document.createElement("input").asInstanceOf[HTMLInputElement]
    input.`type` = "file"
    input.name = "replay_file"
    //<input type="file" accept="text/csv" style="display:none" id="upload-file"/>

    input.onchange = (_: Event) => {
      replayParsed.value = None
      val processOnChange = for{
        file <- {
          val files = input.files
          if(files.length == 1) {
            Right(files(0))
          }
          else
            Left(FileIsNotOne)
        }
        _ <- (file.size,file.name) match {
              case (size,_) if size > 1 * 1024 * 1024 => Left(FileSelectedNotSmallSyze)
              case (_,name) if !name.endsWith(".rep") => Left(FileSelectedWrongType)
              case _ => Right(true)
            }


      }yield{

        val parseReplay = JavaScriptRoutes.controllers.ReplayMatchController.parseReplay(discord1,discord2)
        val playAjax = new PlayAjax(parseReplay)
        val data = new FormData()
        data.append("replay_file", file)
        val futValue = playAjax.callByAjaxWithParser(dyn => read[Either[String,ActionByReplay]](dyn.response.toString), data).map(_.flatten)

        futValue.onComplete {
          case Success(Left(error)) => stateUploadProcess.value = ErrorByServerParsing(error)
          case Success(Right(value)) =>
            stateUploadProcess.value = MatchingUsers
            replayParsed.value = Some(value)
          case Failure(exception) => stateUploadProcess.value = FileErrorReceivingParse(exception.toString)
        }
        FileOnProcessToParse

      }
      stateUploadProcess.value = processOnChange match {
        case Left(v) => v
        case Right(v) => v
      }
    }
    input
  }


  @html
  private def buildInput(value: Int): Binding[HTMLInputElement] = Binding{
    val input: NodeBinding[HTMLInputElement] = <input name="nicks" type="radio" value={s"$value"} />
    input.value.onclick = _ => {
      stateUploadProcess.value = ReadyToSend
    }

    input.bind
  }

  private val selection_Same: Binding[HTMLInputElement] = buildInput(1)

  private val selection_Cross: Binding[HTMLInputElement] = buildInput(2)

  import shared.models.ActionBySmurf._
  @html
  private val correlateTags = Binding {
    replayParsed.bind.map {
      case ActionByReplay(_,Some(smurf1),Some(smurf2),SmurfsEmpty, winner) =>
        <div class="input-field col s12">
          <p>
            <label>
              {selection_Same.bind}<span class="playerRelation">
              {s"[$player1 -> ${smurf1}] y [$player2 -> ${smurf2}]"}
            </span>
            </label>
          </p>
          <p>
            <label>
              {selection_Cross.bind}<span class="playerRelation">
              {s"[$player1 -> ${smurf2}] y [$player2 -> ${smurf1}]"}
            </span>
            </label>
          </p>
          <input type="hidden" name="player1" value={smurf1}/>
          <input type="hidden" name="player2" value={smurf2}/>
          <input type="hidden" name="winner" value={winner.toString}/>
        </div>
        case ActionByReplay(_,player1,player2,ImpossibleToDefine, _) =>
          stateUploadProcess.value = ErrorImpossibleMessage(player1, player2)
          <div>Error</div>
        case ActionByReplay(_,_,_,CorrelatedCruzadoDefined, _) =>
          stateUploadProcess.value = ReadyToSend
          <div>Los nicks ya están definidos :)</div>
        case ActionByReplay(_,_,_,CorrelatedParallelDefined, _) =>
          stateUploadProcess.value = ReadyToSend
          <div>Los nicks ya están definidos :)</div>
        case ActionByReplay(true,Some(_),Some(_),_, _) =>
          stateUploadProcess.value = ReadyToSend
          <div>Puedes enviar el replay! aunque un nick falta ser aprobado por los moderadores. :)</div>
        case ActionByReplay(_,player1,player2,_, _) =>
          stateUploadProcess.value = ErrorImpossibleMessage(player1, player2)
          <div>Error</div>



    }.getOrElse(<div></div>)
  }
  @html
  val buttonSubmit: Binding[Button] = Binding {
    val button: NodeBinding[Button] = <button class="btn waves-effect waves-light" type="submit" name="action">Enviar Replay
      <i class="material-icons right">send</i>
    </button>
    button.value.disabled = stateUploadProcess.bind != ReadyToSend
    button.bind
  }



  @html
  val content: Binding[Node] = {


    <div>

    <div class="btn">
      <span>Replay</span>
      {inputFile}
    </div>

    <div class="file-path-wrapper">
      <input class="file-path validate" type="text"/>
    </div>
    <div>
      {
        messageState.bind
      }
    </div>
    {correlateTags.bind}
    {buttonSubmit.bind}


     <input type="hidden" name="player1Discord" value={discord1}/>
     <input type="hidden" name="player2Discord" value={discord2}/>

    </div>
  }



  def render(): Unit ={
    html.render(fieldDiv, content)
  }


}
object ReplayUpdater{
  def init(): Unit ={
    val divs = document.getElementsByTagName("div")
    val length = divs.length
    val divsUpload = (0 until length).map(i => divs.item(i)).filter(_.id.startsWith("replay_file_field_")).map(_.asInstanceOf[Div])
    divsUpload.foreach{ div =>

      for{
        p1 <- div.dataset.get("player1")
        p2 <- div.dataset.get("player2")
        d1 <- div.dataset.get("discord1")
        d2 <- div.dataset.get("discord2")
      }yield{
        val ru = new ReplayUpdater(div,p1,p2,d1,d2)
        ru.render()
      }

    }
  }
}