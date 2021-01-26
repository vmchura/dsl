import org.scalajs.dom
import org.scalajs.dom.html.{Div, Input}
import org.scalajs.dom.raw.{
  FormData,
  HTMLButtonElement,
  HTMLCollection,
  HTMLFormElement,
  HTMLInputElement,
  HTMLOptionElement,
  HTMLOptionsCollection,
  HTMLSelectElement
}
import shared.models.DiscordPlayerLogged
import upickle.default.read

import scala.collection.mutable
import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}
import scala.util.{Failure, Success}
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

@JSExportTopLevel("Main")
object Main {

  def main(args: Array[String]): Unit = {}
  @JSExport("init")
  def init(): Unit = {

    ReplayUploader.init()

  }
  @JSExport("initMemberFinder")
  def initMemberFinder(): Unit = {
    val container =
      dom.document.getElementById("find-members").asInstanceOf[Div]
    val queryTextInput =
      dom.document
        .getElementById("input-query-id")
        .asInstanceOf[HTMLInputElement]
    val optionsSelect = dom.document
      .getElementById("members-option-id")
      .asInstanceOf[HTMLSelectElement]
    println(s"This is $optionsSelect")
    println(s"This was ${dom.document.getElementById("members-option-id")}")
    val searchButton = dom.document
      .getElementById("search-member-button")
      .asInstanceOf[HTMLButtonElement]

    searchButton.onclick = ae => {
      val optionsOnClick =
        optionsSelect.options.asInstanceOf[HTMLOptionsCollection]

      (1 to optionsOnClick.length).foreach(_ => optionsOnClick.remove(0))

      val call = new PlayAjax(
        JavaScriptRoutes.controllers.teamsystem.MemberSupervisorController
          .findMembers()
      )
      val futValue = call.callByAjaxWithParser(
        dyn =>
          read[Either[String, Seq[DiscordPlayerLogged]]](
            dyn.toString
          ), {
          val form = new FormData()
          form.append("query", queryTextInput.value)
          form
        }
      )
      futValue.map(x => x.flatten).onComplete {
        case Success(Right(value)) =>
          val options = value.map { ch =>
            val u =
              dom.document
                .createElement("option")
                .asInstanceOf[HTMLOptionElement]
            u.text = s"${ch.username}#${ch.discriminator}"
            u.value = ch.discordID.id
            u
          }

          val currentOptions =
            optionsSelect.options.asInstanceOf[HTMLOptionsCollection]
          options.foreach(currentOptions.add)

        case Success(Left(reasonError)) =>
          println(s"ERROR Left($reasonError)")
        case Failure(exception) =>
          println(s"ERROR Failure(${exception.getMessage})")
      }
    }

  }
  def findTokenValue(): String = {
    val formElement =
      dom.document.getElementById("myForm").asInstanceOf[HTMLFormElement]
    val inputelementCollection = formElement.getElementsByTagName("input")
    val inputelement =
      inputelementCollection.namedItem("csrfToken").asInstanceOf[Input]
    inputelement.value
  }
}
