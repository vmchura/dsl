package jobs


import play.api.libs.json.{JsArray, JsString, Json}
import java.util.Base64

import sttp.client._
import javax.inject.Inject
import play.api.Configuration
import java.io.{File, FileInputStream}

import shared.models.ReplayDescriptionShared
class ParseFile @Inject() (configuration: Configuration) {
  private val lambda_x_api_key = configuration.get[String]("lambda.apikey")

  def parseFile(file: File): Either[String,String] = {

    val in = new FileInputStream(file)
    val bytes = new Array[Byte](file.length.toInt)
    in.read(bytes)
    in.close()
    val encoded = Base64.getEncoder.encodeToString(bytes)

    // the `query` parameter is automatically url-encoded
    // `sort` is removed, as the value is not defined
    val request = basicRequest.
      header("x-api-key",lambda_x_api_key).
      body(Json.obj("replayfile" -> JsString(encoded), "filename" -> JsString("este es un archivo X")).toString()).post(uri"https://o1hyykheh4.execute-api.us-east-2.amazonaws.com/default/replayParser")


    implicit val backend: SttpBackend[Identity, Nothing, NothingT] = HttpURLConnectionBackend()


    // alternatively, if you prefer to pass the backend explicitly, instead
    // of using implicits, you can also call:
    val response = backend.send(request)

    response.body


  }
  def parseJsonResponse(stringJson: String): Either[String,ReplayDescriptionShared] = {
    val json = Json.parse(stringJson)
    val playersJson = (json \ "Header" \ "Players").getOrElse(JsArray.empty).asInstanceOf[JsArray]
    case class Player(team: Int, name: String)
    val players = playersJson.value.toList.flatMap{ p =>
      for{
        team <- (p \ "Team").asOpt[Int]
        name <- (p \ "Name").asOpt[String]
      }yield{
        Player(team,name)
      }

    }

    (for{
      p1 <- players.find(_.team ==1).map(_.name)
      p2 <- players.find(_.team ==2).map(_.name)
      winnerTeam <- (json \ "Computed" \ "WinnerTeam").asOpt[Int]
    }yield{
      Right(ReplayDescriptionShared(p1,p2, winnerTeam))
    }).getOrElse(Left("Cant find players"))




  }
}
