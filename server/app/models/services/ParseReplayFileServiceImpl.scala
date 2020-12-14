package models.services

import java.io.{File, FileInputStream}
import java.util.Base64

import javax.inject.Inject
import play.api.Configuration
import play.api.libs.json.{JsString, Json}
import sttp.client._
import sttp.client.asynchttpclient.WebSocketHandler
import sttp.client.asynchttpclient.future.AsyncHttpClientFutureBackend

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ParseReplayFileServiceImpl @Inject() (
    configuration: Configuration
) extends ParseReplayFileService {
  private val lambda_x_api_key = configuration.get[String]("lambda.apikey")

  def parseFile(file: File): Future[Either[String, String]] = {

    val in = new FileInputStream(file)
    val bytes = new Array[Byte](file.length.toInt)
    in.read(bytes)
    in.close()
    val encoded = Base64.getEncoder.encodeToString(bytes)

    // the `query` parameter is automatically url-encoded
    // `sort` is removed, as the value is not defined
    val request = basicRequest
      .header("x-api-key", lambda_x_api_key)
      .body(
        Json
          .obj(
            "replayfile" -> JsString(encoded),
            "filename" -> JsString("este es un archivo X")
          )
          .toString()
      )
      .post(
        uri"https://o1hyykheh4.execute-api.us-east-2.amazonaws.com/default/replayParser"
      )

    implicit val backend: SttpBackend[Future, Nothing, WebSocketHandler] =
      AsyncHttpClientFutureBackend()

    // alternatively, if you prefer to pass the backend explicitly, instead
    // of using implicits, you can also call:
    val response = backend.send(request)

    response.map(_.body)

  }

}
