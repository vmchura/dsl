package models.daos

import javax.inject.Inject
import models.ValidUserSmurf
import play.api.libs.json.Json
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.Cursor
import reactivemongo.play.json.collection.JSONCollection
import shared.models.DiscordID

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import models.ModelsJsonImplicits._
class ValidUserSmurfDAOImpl @Inject() (val reactiveMongoApi: ReactiveMongoApi)
    extends ValidUserSmurfDAO {
  def collection: Future[JSONCollection] =
    reactiveMongoApi.database.map(_.collection("dsl.validsmurf"))

  override def add(
      discordID: DiscordID,
      smurf: models.Smurf
  ): Future[Boolean] = {

    def pushSmurf(jsc: JSONCollection): Future[Boolean] = {
      jsc
        .update(ordered = true)
        .one(
          Json.obj("discordID" -> discordID),
          Json.obj("$push" -> Json.obj("smurfs" -> smurf)),
          upsert = true
        )
        .map(_.ok)
    }
    def addNewUser(jsc: JSONCollection): Future[Boolean] = {
      jsc
        .insert(ordered = true)
        .one(ValidUserSmurf(discordID, List(smurf)))
        .map(_.ok)
    }
    for {
      jsc <- collection
      user <-
        jsc
          .find(
            Json.obj("discordID" -> discordID),
            Option.empty[ValidUserSmurf]
          )
          .one[ValidUserSmurf]
      result <- user match {
        case Some(_) => pushSmurf(jsc)
        case None    => addNewUser(jsc)
      }
    } yield {
      result
    }

  }

  override def load(discordID: DiscordID): Future[Option[ValidUserSmurf]] =
    collection.flatMap(
      _.find(Json.obj("discordID" -> discordID), Option.empty[ValidUserSmurf])
        .one[ValidUserSmurf]
    )

  override def findOwner(smurf: models.Smurf): Future[Option[DiscordID]] =
    collection
      .flatMap(
        _.find(Json.obj("smurfs" -> smurf), Option.empty[ValidUserSmurf])
          .one[ValidUserSmurf]
      )
      .map(_.map(_.discordID))

  override def all(): Future[Seq[ValidUserSmurf]] =
    collection.flatMap(
      _.find(Json.obj(), Option.empty[ValidUserSmurf])
        .cursor[ValidUserSmurf]()
        .collect[List](-1, Cursor.FailOnError[List[ValidUserSmurf]]())
    )
}
