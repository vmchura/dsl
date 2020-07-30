import org.scalajs.dom.XMLHttpRequest
import org.scalajs.dom.ext.Ajax
import org.scalajs.dom.raw.FormData
import shared.PlayCall

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js
import scala.scalajs.js.JSON

class PlayAjax[O](playCall: PlayCall[O]) {

  private def runCallWithParser[U](parseRequest: XMLHttpRequest => Either[String, U], data: FormData = new FormData()): Future[Either[String, U]] = {

    if (playCall.method.equals("POST"))
      Ajax.post(playCall.absoluteURL(), headers = Map("Csrf-Token" -> Main.findTokenValue()), data = data).map(parseRequest)
    else {
      if (playCall.method.equals("GET")) {
        Ajax.get(playCall.absoluteURL()).map(parseRequest)
      } else {
        Future.successful(Left(playCall.method + " NOT SUPPORTED"))
      }
    }
  }

  def runCall(data: FormData = new FormData()): Future[XMLHttpRequest] = {

    if (playCall.method.equals("POST"))
      Ajax.post(playCall.absoluteURL(), headers = Map("Csrf-Token" -> Main.findTokenValue()), data = data)
    else {
      if (playCall.method.equals("GET")) {
        Ajax.get(playCall.absoluteURL())
      } else {
        throw new IllegalArgumentException("No supported: " + playCall.method)
      }
    }
  }
  def callByAjaxWithParser(parser: js.Dynamic => O, data: FormData = new FormData()): Future[Either[String, O]] = {

    def parseRequest(response: XMLHttpRequest): Either[String, O] = {

      try {

        JSON.parse(response.responseText) match {
          case json: js.Dynamic => Right(parser(json))
          case _ => Left("Response can not parse it as JSON: " + response.responseText)
        }
      } catch {
        case e: Throwable => Left("ERROR describing JSON?: " + e.toString)
      }
    }
    runCallWithParser(parseRequest, data)

  }

  def callByAjaxGetText(): Future[Either[String, String]] = {
    def parseRequest(response: XMLHttpRequest): Either[String, String] = if (response.status == 200) {
      Right(response.responseText)
    } else {
      Left(response.responseText)
    }
    runCallWithParser(parseRequest)

  }

}
