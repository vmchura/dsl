package models.daos.teamsystem

import models.daos.SmurfQueryableImpl
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.play.json.collection.JSONCollection

import javax.inject.Inject
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class TeamUsersSmurfDAOImpl @Inject() (val reactiveMongoApi: ReactiveMongoApi)
    extends TeamUserSmurfDAO
    with SmurfQueryableImpl {
  def collection: Future[JSONCollection] =
    reactiveMongoApi.database.map(_.collection("teamsystem.validsmurf"))

}
