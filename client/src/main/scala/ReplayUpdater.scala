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
import shared.models.ActionBySmurf._

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

class ReplayUpdater(fieldDiv: Div, player1: String, player2: String, discord1: String, discord2: String, playerViewing: String) {


  implicit def makeIntellijHappy[T<:org.scalajs.dom.raw.Node](x: scala.xml.Node): Binding[T] =
    throw new AssertionError("This should never execute.")

  abstract class WithMessageResult(private val messageToShow: String){
    def getMessageToShow(): String = messageToShow
  }

  sealed trait StateSettingResult extends WithMessageResult {
    def stateType: String
  }
  sealed trait DangerState extends StateSettingResult {
    override def stateType: String = "danger"
  }
  sealed trait WarningState extends StateSettingResult {
    override def stateType: String = "warning"
  }
  sealed trait SuccessState extends StateSettingResult {
    override def stateType: String = "success"
  }
  sealed trait InfoState extends StateSettingResult {
    override def stateType: String = "info"
  }
  
  object FileUnselected extends WithMessageResult("Seleccione el replay de la partida") with WarningState
  object FileSelectedWrongType extends WithMessageResult("El archivo seleccionado debe terminar en .rep") with DangerState
  object FileSelectedNotSmallSyze extends WithMessageResult("El archivo debe ser menor a 1Mb") with DangerState
  object FileSelectedOk extends WithMessageResult("Archivo seleccionado de manera correcta") with SuccessState
  object FileParsedCorrectly extends WithMessageResult("Archivo leído correctamente") with SuccessState
  case class FileErrorReceivingParse(error: String) extends WithMessageResult(s"Error en la conexión con el servidor, vuelva a intentarlo luego o comuníquese con el admin") with DangerState{
    override def getMessageToShow(): String = {
      println(error)
      if(error.contains("IsAlreadyRegistered"))
        s"${super.getMessageToShow()} / Replay ya está registrada"
      else
        super.getMessageToShow()
    }
  }
  object  FileParsedIncorrectly extends WithMessageResult("El archivo no se pudo interpretar como replay") with DangerState
  object FileOnProcessToParse extends WithMessageResult("Esperando el PRE procesamiento del archivo") with InfoState
  object FileIsNotOne extends WithMessageResult("Sólo se debe escoger UN archivo") with DangerState
  object MatchingUsers extends WithMessageResult("Relacione al usuario con el nick en el juego") with SuccessState
  object ReadyToSend extends WithMessageResult(":) Listo para subir el archivo al servidor") with SuccessState
  case class ErrorByServerParsing(message: String) extends WithMessageResult(s"ERROR en el servidor, posiblemente replay corrupta, comuníquese con el admin") with DangerState{
    override def getMessageToShow(): String = {
      println(message)
      if(message.contains("IsAlreadyRegistered"))
        s"${super.getMessageToShow()} / Replay ya está registrada"
      else
        super.getMessageToShow()
    }
  }
  case class ErrorImpossibleMessage(smurf1: Option[String], smurf2: Option[String]) extends WithMessageResult(
    (smurf1,smurf2) match {
      case (Some(x), Some(y)) => s"El smurf $x o $y ya está asignado a otro usuario. Si crees que es un error, comuníquese con el admin."
      case _ => s"El replay no se pudo interpretar correctamente.  Si crees que es un error, comuníquese con el admin."
    }) with DangerState

  private val stateUploadProcess = Var[StateSettingResult](FileUnselected)
  private val replayParsed = Var[Option[ActionByReplay]](None)
  private val fileNameSelected = Var[Option[String]](None)

  @html
  private def createRelation(primarySecondaryColor: String)(playerName: String, smurf: String) = {
    <p>{playerName}<span class={s"badge rounded-pill bg-$primarySecondaryColor"}>{smurf} </span> </p>
  }

  private def secureRelation(playerName: String, smurf: String) = createRelation("primary")(playerName,smurf)
  private def pendingRelation(playerName: String, smurf: String) = createRelation("secondary")(playerName,smurf)

  @html
  private def createSecureRelations(smurfForPlayer1: String, smurfForPlayer2: String) = {

        <div>
          <span class="font-weight-bold">Jugadores:</span>
          {secureRelation(player1,smurfForPlayer1)}
          {secureRelation(player2,smurfForPlayer2)}
        </div>
  }

  @html
  private def createSecureForFirstPlayer(smurfForPlayer1: String, smurfForPlayer2: String) = {

        <div>
          <span class="font-weight-bold">Jugadores:</span>
          {secureRelation(player1,smurfForPlayer1)}
          {pendingRelation(player2,smurfForPlayer2)}
        </div>
  }
  @html
  private def createSecureForSecondPlayer(smurfForPlayer1: String, smurfForPlayer2: String) = {

        <div>
          <span class="font-weight-bold">Jugadores:</span>
          {pendingRelation(player1,smurfForPlayer1)}
          {secureRelation(player2,smurfForPlayer2)}
        </div>
  }


  @html
  def nameAndWinner(mapName: String, winner: Int, smurf1: String, smurf2: String) = {
    <div>
      <span> <span class="font-weight-bold">Mapa:</span> {mapName}</span>
    </div>
    <div>
      <span> <span class="font-weight-bold">Ganador:</span> {if(winner==1) smurf1 else smurf2}</span>
    </div>

  }
  @html
  def buildContainer(mapName: String, winner: Int, smurf1: String, smurf2: String)(content: Binding[Node]): Binding[Node] = {
    <div class="container emptysmurfcontainer">
      {nameAndWinner(mapName,winner,smurf1,smurf2)}
      <div class="smurfs">
        {content}
      </div>
      <div class="alert alert-info" data:role="alert">
        Si consideras que existe un error, comunícate con el admin de la plataforma.
      </div>
    </div>
  }

  @html
  def buildInput(playerViewing: Int,option: Int, idForInput: String) = {
    val valueIfClicked = if(playerViewing == option) "1" else "2"
    val input: NodeBinding[HTMLInputElement] = <input type="radio" class="form-check-input" name="nicks" id={idForInput} autocomplete="off" value={valueIfClicked}/>
    input.value.onclick = _ => stateUploadProcess.value = ReadyToSend

    input

  }

  @html
  private def buildBoxSmurfs(actionBySmurf: ActionBySmurf, smurf1: String, smurf2: String, winner: Int, mapName: String) = {

    def bc(content: Binding[Node]) = buildContainer(mapName, winner, smurf1,smurf2)(content)
    val emptyDiv: Binding[Node] = <div></div>
    playerViewing.toIntOption.fold(emptyDiv){ playerQuerying =>
      actionBySmurf match {
        case SmurfsEmpty =>
          bc(
            <div>
              <span>Hola <span class="font-weight-bold">{if(playerQuerying==1) player1 else player2}</span>,
                ¿Con qué nombre jugaste esta partida?</span>
              <div class="form-check">
                {buildInput(playerQuerying,1,"firstOptionID1")}
                <label class="form-check-label" for="firstOptionID1">
                {smurf1}
                </label>
              </div>
              <div class="form-check">
                {buildInput(playerQuerying,2,"firstOptionID2")}
                <label class="form-check-label" for="firstOptionID2">
                  {smurf2}
                </label>
              </div>
              <input type="hidden" name="player1" value={smurf1}/>
              <input type="hidden" name="player2" value={smurf2}/>
              <input type="hidden" name="winner" value={winner.toString}/>
            </div>
          )
        case CorrelatedParallelDefined => bc(createSecureRelations(smurf1, smurf2))
        case CorrelatedCruzadoDefined => bc(createSecureRelations(smurf2, smurf1))
        case Correlated1d1rDefined => bc(createSecureForFirstPlayer(smurf1, smurf2))
        case Correlated1d2rDefined => bc(createSecureForFirstPlayer(smurf2, smurf1))
        case Correlated2d1rDefined => bc(createSecureForSecondPlayer(smurf2, smurf1))
        case Correlated2d2rDefined => bc(createSecureForSecondPlayer(smurf1, smurf2))
        case _ => emptyDiv
      }
    }

  }

  @html
  private val messageState = Binding{stateUploadProcess.bind.getMessageToShow}



  @html
  private val inputFile = {
    val input: HTMLInputElement = org.scalajs.dom.document.createElement("input").asInstanceOf[HTMLInputElement]
    input.`type` = "file"
    input.name = "replay_file"
    input.classList.add("form-file-input")
    input.id = "replayFileID"
    //<input type="file" accept="text/csv" style="display:none" id="upload-file"/>

    input.onchange = (_: Event) => {
      replayParsed.value = None
      fileNameSelected.value = None
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
        fileNameSelected.value = Some(file.name)

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
  private val correlateTags = Binding {
    replayParsed.bind.map { acbrep =>
      val ActionByReplay(_, smurf1, smurf2, actionBySmurf ,winner,mapName) = acbrep
      (smurf1, smurf2) match {
        case (Some(smurfPlayer1),Some(smurfPlayer2)) =>
          actionBySmurf match {
            case SmurfsEmpty =>
              buildBoxSmurfs(actionBySmurf, smurfPlayer1, smurfPlayer2, winner, mapName)
            case Correlated1d1rDefined |
                 Correlated2d2rDefined |
                 Correlated1d2rDefined |
                 Correlated2d1rDefined |
                 CorrelatedParallelDefined |
                 CorrelatedCruzadoDefined =>

              stateUploadProcess.value = ReadyToSend
              buildBoxSmurfs(actionBySmurf, smurfPlayer1, smurfPlayer2, winner, mapName)


            case ImpossibleToDefine =>
              stateUploadProcess.value = ErrorImpossibleMessage(Some(smurfPlayer1), Some(smurfPlayer2))
              <div>Error</div>
          }
        case _ =>
          stateUploadProcess.value = ErrorImpossibleMessage(smurf1, smurf2)
          <div>Error</div>
      }



    }.getOrElse(<div></div>)
  }
  @html
  val buttonSubmit: Binding[Button] = Binding {
    val button: NodeBinding[Button] = <button class="btn btn-primary" type="submit" name="action">Enviar Replay
      <i class="material-icons right">send</i>
    </button>
    button.value.disabled = stateUploadProcess.bind != ReadyToSend
    button.bind
  }



  @html
  val content: Binding[Node] = {


    <div class="container">

      <div class="form-file">
          {inputFile}
          <label class="form-file-label" for="replayFileID">
            <span class="form-file-text">{fileNameSelected.bind.getOrElse("Replay ...")}</span>
            <span class="form-file-button">Seleccionar replay</span>
          </label>
      </div>
      {
        stateUploadProcess.bind match {
          case _: DangerState | _: InfoState =>
            <div class={s"alert alert-${stateUploadProcess.bind.stateType}"} data:role="alert">
              {
              messageState.bind
              }
            </div>
          case _ =>
            <div></div>
        }
      }


      {correlateTags}
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
        pv <- div.dataset.get("playerviewing")
      }yield{
        val ru = new ReplayUpdater(div,p1,p2,d1,d2,pv)
        ru.render()
      }

    }
  }
}
