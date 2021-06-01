package models

import org.joda.time.DateTime
import play.api.libs.json.Json
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.play.json.collection.JSONCollection

import java.util.UUID
import javax.inject.Inject
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
case class PostDataFromUsage(clientID: UUID, actionType: String)
object PostDataFromUsage {
  implicit val jsonFormat = Json.format[PostDataFromUsage]
}
case class PostDataResponse(
    clientID: UUID,
    continue: Boolean,
    reason: String,
    currentDate: String
)
object PostDataResponse {
  implicit val jsonFormat = Json.format[PostDataResponse]
}
class PrismarisDAOTestTemp @Inject() (val reactiveMongoApi: ReactiveMongoApi) {
  def collection: Future[JSONCollection] =
    reactiveMongoApi.database.map(_.collection("prismaris.logg"))
  def addLog(dataInput: PostDataFromUsage): Future[PostDataResponse] = {
    for {
      input <- collection.flatMap(
        _.insert(ordered = true).one(dataInput)
      )
      output <- collection.flatMap(
        _.insert(ordered = true)
          .one(
            PostDataResponse(
              dataInput.clientID,
              continue = true,
              dataInput.actionType,
              DateTime.now().toString()
            )
          )
      )
    } yield {
      PostDataResponse(
        dataInput.clientID,
        continue = true,
        dataInput.actionType,
        DateTime.now().toString()
      )
    }
  }
}
