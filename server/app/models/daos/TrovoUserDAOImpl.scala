package models.daos
import models.{TrovoUser, TrovoUserID}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.libs.json.Json
import play.modules.reactivemongo._
import reactivemongo.api.Cursor
import reactivemongo.play.json.collection.JSONCollection
import reactivemongo.play.json.compat._

import javax.inject.Inject
class TrovoUserDAOImpl @Inject() (val reactiveMongoApi: ReactiveMongoApi)
    extends TrovoUserDAO {

  def collection: Future[JSONCollection] =
    reactiveMongoApi.database.map(_.collection("trovo.user"))

  override def find(discordID: String): Future[Option[TrovoUser]] = {
    val query = Json.obj("discordID" -> discordID)
    collection.flatMap(_.find(query, Option.empty[TrovoUser]).one[TrovoUser])
  }

  override def find(trovoUserID: TrovoUserID): Future[Option[TrovoUser]] = {
    val query = Json.obj("trovoUserID" -> trovoUserID)
    collection.flatMap(_.find(query, Option.empty[TrovoUser]).one[TrovoUser])
  }

  override def save(trovoUser: TrovoUser): Future[Option[TrovoUser]] =
    collection
      .flatMap(
        _.insert(ordered = true)
          .one(
            trovoUser
          )
      )
      .map { res =>
        Option.when(res.n == 1 && res.ok)(trovoUser)
      }

  override def update(trovoUser: TrovoUser): Future[Option[TrovoUser]] =
    collection
      .flatMap(
        _.update(ordered = true)
          .one(
            Json.obj("discordID" -> trovoUser.discordID),
            trovoUser,
            upsert = true
          )
      )
      .map { res =>
        Option.when(res.n == 1 && res.ok)(trovoUser)
      }

  override def remove(discordID: String): Future[Boolean] =
    collection
      .flatMap(
        _.insert(ordered = true)
          .one(
            Json.obj("discordID" -> discordID)
          )
      )
      .map { _.ok }

  override def all(): Future[List[TrovoUser]] =
    collection.flatMap(
      _.find(Json.obj(), Option.empty[TrovoUser])
        .cursor[TrovoUser]()
        .collect[List](-1, Cursor.FailOnError[List[TrovoUser]]())
    )

}
