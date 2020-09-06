import org.lrng.binding.html
import org.scalajs.dom.html.{Button, Div}
import com.thoughtworks.binding.Binding.{BindingSeq, Var}
import com.thoughtworks.binding.Binding
import org.lrng.binding.html.NodeBinding
import org.scalajs.dom.raw.{Event, FormData, HTMLInputElement}

import scala.util.{Failure, Success}
import org.scalajs.dom.{Event, Node, document, window}
import shared.models.ActionByReplay
import upickle.default.read
import shared.models.ActionBySmurf._

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

class ReplayUpdater(fieldDiv: Div, player1: String, player2: String, discord1: String, discord2: String) {


  implicit def makeIntellijHappy[T<:org.scalajs.dom.raw.Node](x: scala.xml.Node): Binding[T] =
    throw new AssertionError("This should never execute.")

  abstract class WithMessageResult(val messageToShow: String)

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
  case class FileErrorReceivingParse(error: String) extends WithMessageResult(s"Error en la conexión con el servidor, vuelva a intentarlo luego o comuníquese con el admin") with DangerState
  object  FileParsedIncorrectly extends WithMessageResult("El archivo no se pudo interpretar como replay") with DangerState
  object FileOnProcessToParse extends WithMessageResult("Esperando el PRE procesamiento del archivo") with InfoState
  object FileIsNotOne extends WithMessageResult("Sólo se debe escoger UN archivo") with WarningState
  object MatchingUsers extends WithMessageResult("Relacione al usuario con el nick en el juego") with InfoState
  object ReadyToSend extends WithMessageResult(":) Listo para subir el archivo al servidor") with SuccessState
  case class ErrorByServerParsing(message: String) extends WithMessageResult(s"ERROR en el servidor, posiblemente replay corrupta, comuníquese con el admin") with DangerState
  case class ErrorImpossibleMessage(smurf1: Option[String], smurf2: Option[String]) extends WithMessageResult(
    (smurf1,smurf2) match {
      case (Some(x), Some(y)) => s"El smurf $x o $y ya está asignado a otro usuario. Si crees que es un error, comuníquese con el admin."
      case _ => s"El replay no se pudo interpretar correctamente.  Si crees que es un error, comuníquese con el admin."
    }) with DangerState

  private val stateUploadProcess = Var[StateSettingResult](FileUnselected)
  private val replayParsed = Var[Option[ActionByReplay]](None)
  private val fileNameSelected = Var[Option[String]](None)

  @html
  private def createRelation(primarySecondaryColor: String, iconString: String)(playerName: String, smurf: String) = {
    <p>{playerName}<span class={s"badge rounded-pill bg-$primarySecondaryColor"}>{smurf} </span> <i class="material-icons">
      {iconString}</i></p>
  }

  private def secureRelation(playerName: String, smurf: String) = createRelation("primary","sentiment_very_satisfied")(playerName,smurf)
  private def pendingRelation(playerName: String, smurf: String) = createRelation("secondary","sentiment_neutral")(playerName,smurf)

  @html
  private def createSecureRelations(smurfForPlayer1: String, smurfForPlayer2: String) = {

        <div>
          {secureRelation(player1,smurfForPlayer1)}
          {secureRelation(player2,smurfForPlayer2)}
        </div>
  }

  @html
  private def createSecureForFirstPlayer(smurfForPlayer1: String, smurfForPlayer2: String) = {

        <div>
          {secureRelation(player1,smurfForPlayer1)}
          {pendingRelation(player2,smurfForPlayer2)}
        </div>
  }
  @html
  private def createSecureForSecondPlayer(smurfForPlayer1: String, smurfForPlayer2: String) = {

        <div>
          {pendingRelation(player1,smurfForPlayer1)}
          {secureRelation(player2,smurfForPlayer2)}
        </div>
  }


  @html
  def nameAndWinner(mapName: String, winner: Int, smurf1: String, smurf2: String) = {
    <div class="mapname">{mapName}</div>
      <div class="winner">{if(winner==1) smurf1 else smurf2}</div>
  }
  @html
  def buildContainer(mapName: String, winner: Int, smurf1: String, smurf2: String)(content: Binding[Node]): Binding[Node] = {
    <div class="container emptysmurfcontainer">
      {nameAndWinner(mapName,winner,smurf1,smurf2)}
      <div class="smurfs">
        {content}
      </div>
      <div class="smurfsmessage">
        Si consideras que existe un error, comunícate con el admin de la plataforma.
      </div>
    </div>
  }

  private val selection_Same: Binding[HTMLInputElement] = buildInput(1,"radioSelectID1")

  private val selection_Cross: Binding[HTMLInputElement] = buildInput(2,"radioSelectID2")

  private def selectChoice(selection: Option[Int]): Unit = Binding{
    selection match {
      case Some(1) => selection_Same.bind.select()
      case Some(2) => selection_Cross.bind.select()
      case _ => ()

    }
  }
  private def selectParallel(): Unit = selectChoice(Some(1))
  private def selectCruzado(): Unit = selectChoice(Some(2))



  @html
  private def buildBoxSmurfs(actionBySmurf: ActionBySmurf, smurf1: String, smurf2: String, winner: Int, mapName: String) = {

    def bc(content: Binding[Node]) = buildContainer(mapName, winner, smurf1,smurf2)(content)
    println(actionBySmurf)
    actionBySmurf match {
      case SmurfsEmpty =>
        bc(
          <div>
          <div class="container-smurfs-select">
            <div class="card-question">¿Cuál es la relación correcta entre nombre Discord y smurf/nick usado en el juego?</div>
            <div class="smurf-container-option">
              <div class="smurf-user-name">{player1}</div>
              <div class="smurf-optionLeft">
                <button onclick={_: Event => selectParallel()}>{smurf1}</button>
              </div>
              <div class="smurf-optionRight">
                <button onclick={_: Event => selectCruzado()}>{smurf2}</button>
              </div>
            </div>
            <div class="smurf-container-option">
              <div class="smurf-user-name">{player2}</div>
              <div class="smurf-optionLeft">
                <button onclick={_: Event => selectCruzado()}>{smurf1}</button>
              </div>
              <div class="smurf-optionRight">
                <button onclick={_: Event => selectParallel()}>{smurf2}</button>
              </div>
            </div>
          </div>
          {selection_Same.bind}
          {selection_Cross.bind}
          <input type="hidden" name="player1" value={smurf1}/>
          <input type="hidden" name="player2" value={smurf2}/>
          <input type="hidden" name="winner" value={winner.toString}/>
          </div>
        )
      case CorrelatedParallelDefined => bc(createSecureRelations(smurf1,smurf2))
      case CorrelatedCruzadoDefined => bc(createSecureRelations(smurf2,smurf1))
      case Correlated1d1rDefined => bc(createSecureForFirstPlayer(smurf1,smurf2))
      case Correlated1d2rDefined => bc(createSecureForFirstPlayer(smurf2,smurf1))
      case Correlated2d1rDefined => bc(createSecureForSecondPlayer(smurf2,smurf1))
      case Correlated2d2rDefined => bc(createSecureForSecondPlayer(smurf1,smurf2))
      case _ => <div></div>
    }

  }

  @html
  private val messageState = Binding{stateUploadProcess.bind.messageToShow}



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

        val futValue = playAjax.callByAjaxWithParser(dyn => read[Either[String,ActionByReplay]]({
          println(dyn.response.toString)
          dyn.response.toString
        }), data).map(_.flatten)


        futValue.onComplete {
          case Success(Left(error)) => stateUploadProcess.value = ErrorByServerParsing(error)
          case Success(Right(value)) =>
            stateUploadProcess.value = MatchingUsers
            println(value)
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
  private def buildInput(value: Int,id: String): Binding[HTMLInputElement] = Binding{
    val input: NodeBinding[HTMLInputElement] = <input class="btn-check"  name="nicks" type="hidden" value={s"$value"} id={id}  />
    input.value.onselect = _ => {
      stateUploadProcess.value = ReadyToSend
    }



    input.bind
  }



  @html
  private val correlateTags = Binding {
    replayParsed.bind.map { acbrep =>
      println(acbrep)
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
      <div class={s"alert alert-${stateUploadProcess.bind.stateType}"} data:role="alert">
        {
        messageState.bind
        }
      </div>

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
      }yield{
        val ru = new ReplayUpdater(div,p1,p2,d1,d2)
        ru.render()
      }

    }
  }
}
