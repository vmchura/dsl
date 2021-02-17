package models.daos

import javax.inject.Inject
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.play.json.collection.JSONCollection

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
class ValidUserSmurfDAOImpl @Inject() (val reactiveMongoApi: ReactiveMongoApi)
    extends ValidUserSmurfDAO
    with SmurfQueryableImpl {
  def collection: Future[JSONCollection] =
    reactiveMongoApi.database.map(_.collection("dsl.validsmurf"))

}
