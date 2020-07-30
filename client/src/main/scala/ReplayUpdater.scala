import org.lrng.binding.html
import org.scalajs.dom.html.{Button, Div}
import com.thoughtworks.binding.Binding.Var
import com.thoughtworks.binding.Binding
import org.lrng.binding.html.NodeBinding
import org.scalajs.dom.raw.{FormData, HTMLInputElement}

import scala.util.{Failure, Success}
import org.scalajs.dom.{Event, Node, document}
import shared.models.ReplayDescriptionShared
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

  private val stateUploadProcess = Var[StateSettingResult](FileUnselected)
  private val replayParsed = Var[Option[ReplayDescriptionShared]](None)


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

        val parseReplay = JavaScriptRoutes.controllers.ReplayMatchController.parseReplay()
        val playAjax = new PlayAjax(parseReplay)
        val data = new FormData()
        data.append("replay_file", file)
        val futValue = playAjax.callByAjaxWithParser(dyn => read[Either[String,ReplayDescriptionShared]](dyn.response.toString), data).map(_.flatten)

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

  @html
  private val correlateTags = Binding {
    replayParsed.bind.map(replay =>

      <div class="input-field col s12">
        <p>
          <label>
            {selection_Same.bind}
            <span class="playerRelation">{s"[$player1 -> ${replay.player1}] y [$player2 -> ${replay.player2}]"}</span>
          </label>
        </p>
        <p>
          <label>
            {selection_Cross.bind}
            <span class="playerRelation">{s"[$player1 -> ${replay.player2}] y [$player2 -> ${replay.player1}]"}</span>
          </label>
        </p>
        <input type="hidden" name="player1" value={replay.player1}/>
        <input type="hidden" name="player2" value={replay.player2}/>
        <input type="hidden" name="winner" value={replay.winner.toString}/>
      </div>

    ).getOrElse(<div></div>)
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