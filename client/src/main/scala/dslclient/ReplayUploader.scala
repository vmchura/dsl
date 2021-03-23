package dslclient

import backendprotocol.{JavaScriptRoutes, PlayAjax}
import com.thoughtworks.binding.Binding
import com.thoughtworks.binding.Binding.Var
import org.lrng.binding.html
import org.lrng.binding.html.NodeBinding
import org.scalajs.dom.html.{Button, Div}
import org.scalajs.dom.raw.{FormData, HTMLInputElement}
import org.scalajs.dom.{Event, Node, document}
import shared.models._
import upickle.default.read

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.util.{Failure, Success}

trait ReplayUploader {
  import ReplayUploader._
  def fieldDiv: Div
  def player1: String
  def player2: String
  def discord1: String
  def discord2: String
  def buttonDiv: Div
  private val playerQuerying = PlayerNameDiscordID(player1, DiscordID(discord1))
  private val theOtherPlayer = PlayerNameDiscordID(player2, DiscordID(discord2))

  private val stateUploadProcess = Var[StateSettingResult](FileUnselected)
  private val replayParsed = Var[Option[ChallongeOneVsOneMatchGameResult]](None)
  private val fileNameSelected = Var[Option[String]](None)

  val optionSelected: Var[Boolean] = Var(false)
  val discordIdsSmurf: Var[Option[Array[String]]] = Var(None)
  def hiddenInputValuesSelected: Binding[Node]

  def nameForInput: String
  def prefixToUserNameIfEmpty: String
  def jugarConjugation: String

  @html
  val hiddenInputValues: Binding[Node] = Binding {
    if (optionSelected.bind) {
      hiddenInputValuesSelected.bind
    } else {
      <div></div>.bind
    }
  }
  @html
  private def createRelation(
      primarySecondaryColor: String
  )(playerName: String, smurf: String) = {
    <p>{playerName}<span class={
      s"bg-$primarySecondaryColor-700 px-1 text-gray-200 rounded"
    }>{smurf} </span> </p>
  }

  private def secureRelation(playerName: String, smurf: String) =
    createRelation("primary")(playerName, smurf)
  private def pendingRelation(playerName: String, smurf: String) =
    createRelation("secondary")(playerName, smurf)

  private def createRelationPlayer(playerDefined: ChallongePlayerDefined) = {
    val name =
      if (
        playerDefined.discordID.discordIDValue.equals(
          playerQuerying.discordID.discordID
        )
      ) playerQuerying.name
      else theOtherPlayer.name

    playerDefined.discordID.withSource match {
      case DiscordByHistory(_) =>
        secureRelation(name, playerDefined.player.smurf)
      case DiscordByLogic(_) =>
        pendingRelation(name, playerDefined.player.smurf)
    }
  }

  @html
  private def createRelationSmarter(
      oneVsOneDefined: ChallongeOneVsOneDefined
  ) = {

    val winnerRelation = createRelationPlayer(oneVsOneDefined.winner)

    val loserRelation = createRelationPlayer(oneVsOneDefined.loser)
    <div>
      <span class="font-bold">Jugadores:</span>
      {winnerRelation}
      {loserRelation}
    </div>
  }

  @html
  private def nameAndWinnerSmarter(
      oneVsOne: ChallongeOneVsOneMatchGameResult
  ) = {
    <div>
      <span> <span class="font-bold">Mapa:</span> ***</span>
    </div>
      <div>
        <span> <span class="font-bold">Ganador:</span> {
      s"${oneVsOne.winner.player.smurf} - ${oneVsOne.winner.player.race}"
    }</span>
      </div>

  }
  @html
  def buildContainerSmarter(
      oneVsOne: ChallongeOneVsOneMatchGameResult
  )(content: Binding[Node]): Binding[Node] = {
    <div class="container mx-auto">
      {nameAndWinnerSmarter(oneVsOne)}
      <div class="smurfs">
        {content}
      </div>
      <div class="bg-indigo-100 border border-indigo-400 text-blue-700 px-4 py-3 rounded relative" data:role="alert">
        <span class="block sm:inline">Si consideras que existe un error, comunícate con el admin de la plataforma.</span>
      </div>
    </div>
  }

  @html
  private def buildInputDynamic(
      smurf: String,
      idForInput: String,
      idsSMurfs: Array[String]
  ) = {
    val input: NodeBinding[HTMLInputElement] =
      <input type="radio" class="form-check-input" name={nameForInput} id={
        idForInput
      } autocomplete="off" value={smurf}/>
    input.value.onclick = _ => {
      discordIdsSmurf.value = Some(idsSMurfs)
      stateUploadProcess.value = ReadyToSend
      optionSelected.value = true
    }

    input
  }

  @html
  private def buildBoxSmurfsSmarter(
      oneVsOne: ChallongeOneVsOneMatchGameResult
  ) = {
    def bc(content: Binding[Node]) =
      buildContainerSmarter(oneVsOne)(content)
    val emptyDiv: Binding[Node] = <div></div>
    val componentIfSmurfsEmpty = {
      <div>
        <span>
          {prefixToUserNameIfEmpty} <span class="font-weight-bold">{
        playerQuerying.name
      }</span>,
          ¿Con qué nombre
          {jugarConjugation} esta partida?</span>
        <div class="form-check">
          {
        buildInputDynamic(
          oneVsOne.winner.player.smurf,
          "firstOptionID1",
          Array(
            playerQuerying.discordID.discordID,
            oneVsOne.winner.player.smurf,
            theOtherPlayer.discordID.discordID,
            oneVsOne.loser.player.smurf
          )
        )
      }
          <label class="form-check-label" for="firstOptionID1">
            {oneVsOne.winner.player.smurf}
          </label>
        </div>
        <div class="form-check">
          {
        buildInputDynamic(
          oneVsOne.loser.player.smurf,
          "firstOptionID2",
          Array(
            playerQuerying.discordID.discordID,
            oneVsOne.loser.player.smurf,
            theOtherPlayer.discordID.discordID,
            oneVsOne.winner.player.smurf
          )
        )
      }
          <label class="form-check-label" for="firstOptionID2">
            {oneVsOne.loser.player.smurf}
          </label>
        </div>
      </div>
    }

    (
      oneVsOne.winner.discordID.map(_.withSource),
      oneVsOne.loser.discordID.map(_.withSource)
    ) match {
      case (Left(_), _) => emptyDiv
      case (_, Left(_)) => emptyDiv
      case (Right(EmptyDiscordID), Right(EmptyDiscordID)) =>
        bc(componentIfSmurfsEmpty)
      case (Right(winnerID), Right(loserID)) =>
        bc(
          createRelationSmarter(
            ChallongeOneVsOneDefined(
              ChallongePlayerDefined(
                winnerID.buildDefined(),
                oneVsOne.winner.player
              ),
              ChallongePlayerDefined(
                loserID.buildDefined(),
                oneVsOne.loser.player
              )
            )
          )
        )
    }
  }

  @html
  private val messageState = Binding {
    stateUploadProcess.bind.getMessageToShow
  }

  @html
  private val inputFile = {
    val input: HTMLInputElement = org.scalajs.dom.document
      .createElement("input")
      .asInstanceOf[HTMLInputElement]
    input.`type` = "file"
    input.name = "replay_file"
    input.classList.add("form-file-input")
    input.id = "replayFileID"
    //<input type="file" accept="text/csv" style="display:none" id="upload-file"/>

    input.onchange = (_: Event) => {
      replayParsed.value = None
      fileNameSelected.value = None
      val processOnChange = for {
        file <- {
          val files = input.files
          if (files.length == 1) {
            Right(files(0))
          } else
            Left(FileIsNotOne)
        }
        _ <- (file.size, file.name) match {
          case (size, _) if size > 1 * 1024 * 1024 =>
            Left(FileSelectedNotSmallSyze)
          case (_, name) if !name.endsWith(".rep") =>
            Left(FileSelectedWrongType)
          case _ => Right(true)
        }

      } yield {
        fileNameSelected.value = Some(file.name)

        val parseReplay = JavaScriptRoutes.controllers.ReplayMatchController
          .parseReplay(discord1, discord2)
        val playAjax = new PlayAjax(parseReplay)
        val data = new FormData()
        data.append("replay_file", file)

        val futValue = playAjax
          .callByAjaxWithParser(
            dyn =>
              read[Either[String, ChallongeOneVsOneMatchGameResult]](
                dyn.response.toString
              ),
            data
          )
          .map(_.flatten)

        futValue.onComplete {
          case Success(Left(error)) =>
            stateUploadProcess.value = ErrorByServerParsing(error)
          case Success(Right(value)) =>
            stateUploadProcess.value = MatchingUsers
            replayParsed.value = Some(value)
          case Failure(exception) =>
            stateUploadProcess.value =
              FileErrorReceivingParse(exception.toString)
        }
        FileOnProcessToParse

      }
      stateUploadProcess.value = processOnChange match {
        case Left(v)  => v
        case Right(v) => v
      }
    }
    input
  }

  @html
  private val correlateTags = Binding {
    replayParsed.bind
      .map { acbrep =>
        val ChallongeOneVsOneMatchGameResult(
          ChallongePlayer(discordIDWinner, playerWinner),
          ChallongePlayer(discordIDLoser, playerLoser)
        ) =
          acbrep
        (discordIDWinner.map(_.isEmpty), discordIDLoser.map(_.isEmpty)) match {
          case (Left(_), _) | (_, Left(_)) =>
            stateUploadProcess.value = ErrorImpossibleMessage(
              Some(playerWinner.smurf),
              Some(playerLoser.smurf)
            )
            <div>Imposible de referir a un único jugador</div>
          case (Right(false), _) | (_, Right(false)) =>
            stateUploadProcess.value = ReadyToSend
            buildBoxSmurfsSmarter(acbrep)
          case _ => buildBoxSmurfsSmarter(acbrep)

        }

      }
      .getOrElse(<div></div>)
  }
  @html
  val buttonSubmit = Binding {
    val button: NodeBinding[Button] =
      <button  type="submit" name="action">
        <span>Enviar Replay</span>
      </button>
    button.value.disabled = stateUploadProcess.bind != ReadyToSend
    button.value.className =
      if (stateUploadProcess.bind == ReadyToSend) "btn-continue"
      else "btn-continue-disabled"

    button.bind
  }

  @html
  val content: Binding[Node] = {

    <div class="container">

      <div class="form-file">
          {inputFile}
          <label class="form-file-label" for="replayFileID">
            <span class="form-file-text">{
      fileNameSelected.bind.getOrElse("Replay ...")
    }</span>
            <span class="form-file-button">Seleccionar replay</span>
          </label>
        {correlateTags}
        {hiddenInputValues}
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




    </div>
  }

  def render(): Unit = {
    html.render(buttonDiv, buttonSubmit)
    html.render(fieldDiv, content)
  }

}
object ReplayUploader {
  case class DiscordID(discordID: String)
  case class PlayerNameDiscordID(name: String, discordID: DiscordID)
  implicit def makeIntellijHappy[T <: org.scalajs.dom.raw.Node](
      x: scala.xml.Node
  ): Binding[T] =
    throw new AssertionError("This should never execute.")
  implicit def makeIntellijHappy2[T <: org.scalajs.dom.raw.Node](
      x: scala.xml.Elem
  ): NodeBinding[T] =
    throw new AssertionError("This should never execute.")

  abstract class WithMessageResult(private val messageToShow: String) {
    def getMessageToShow: String = messageToShow
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

  object FileUnselected
      extends WithMessageResult("Seleccione el replay de la partida")
      with WarningState
  object FileSelectedWrongType
      extends WithMessageResult("El archivo seleccionado debe terminar en .rep")
      with DangerState
  object FileSelectedNotSmallSyze
      extends WithMessageResult("El archivo debe ser menor a 1Mb")
      with DangerState
  object FileSelectedOk
      extends WithMessageResult("Archivo seleccionado de manera correcta")
      with SuccessState
  object FileParsedCorrectly
      extends WithMessageResult("Archivo leído correctamente")
      with SuccessState
  case class FileErrorReceivingParse(error: String)
      extends WithMessageResult(
        s"Error en la conexión con el servidor, vuelva a intentarlo luego o comuníquese con el admin"
      )
      with DangerState {
    override def getMessageToShow: String = {
      println(error)
      if (error.contains("IsAlreadyRegistered"))
        s"${super.getMessageToShow} / Replay ya está registrada"
      else
        super.getMessageToShow
    }
  }
  object FileParsedIncorrectly
      extends WithMessageResult("El archivo no se pudo interpretar como replay")
      with DangerState
  object FileOnProcessToParse
      extends WithMessageResult("Esperando el PRE procesamiento del archivo")
      with InfoState
  object FileIsNotOne
      extends WithMessageResult("Sólo se debe escoger UN archivo")
      with DangerState
  object MatchingUsers
      extends WithMessageResult("Relacione al usuario con el nick en el juego")
      with SuccessState
  object ReadyToSend
      extends WithMessageResult(":) Listo para subir el archivo al servidor")
      with SuccessState
  case class ErrorByServerParsing(message: String)
      extends WithMessageResult(
        s"ERROR en el servidor, posiblemente replay corrupta, comuníquese con el admin"
      )
      with DangerState {
    override def getMessageToShow: String = {
      println(message)
      if (message.contains("IsAlreadyRegistered"))
        s"${super.getMessageToShow} / Replay ya está registrada"
      else
        super.getMessageToShow
    }
  }
  case class ErrorImpossibleMessage(
      smurf1: Option[String],
      smurf2: Option[String]
  ) extends WithMessageResult((smurf1, smurf2) match {
        case (Some(x), Some(y)) =>
          s"El smurf $x o $y ya está asignado a otro usuario. Si crees que es un error, comuníquese con el admin."
        case _ =>
          s"El replay no se pudo interpretar correctamente.  Si crees que es un error, comuníquese con el admin."
      })
      with DangerState

  def init(): Unit = {
    val divs = document.getElementsByTagName("div")
    val length = divs.length
    def initByContext(
        divIDPrefix: String,
        builder: (Div, String, String, String, String, Div) => ReplayUploader
    ): Unit = {
      val divsUpload = (0 until length)
        .map(i => divs.item(i))
        .filter(_.id.startsWith(divIDPrefix))
        .map(_.asInstanceOf[Div])
      divsUpload.foreach { div =>
        val divButton = document
          .getElementById(s"btn-${div.id.substring(divIDPrefix.length)}")
          .asInstanceOf[Div]
        for {
          p1 <- div.dataset.get("player1")
          p2 <- div.dataset.get("player2")
          d1 <- div.dataset.get("discord1")
          d2 <- div.dataset.get("discord2")

        } yield {
          val ru =
            builder(div, p1, p2, d1, d2, divButton)
          ru.render()
        }

      }
    }
    initByContext("player_replay_file_field_", ReplayUploaderByPlayer.apply)
    initByContext("admin_replay_file_field_", ReplayUploaderByAdmin.apply)

  }
}
