package models.daos

import models.{DiscordPlayerLogged, ValidUserSmurf}
import play.api.libs.json.{JsString, Json}
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.Cursor
import reactivemongo.play.json.collection.JSONCollection
import shared.models.DiscordID

import javax.inject.Inject
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DiscordPlayerLoggedDAOImpl @Inject() (
    val reactiveMongoApi: ReactiveMongoApi
) extends DiscordPlayerLoggedDAO {
  def collection: Future[JSONCollection] =
    reactiveMongoApi.database.map(_.collection("teamsystem.discordlogged"))

  override def add(discordPlayerLogged: DiscordPlayerLogged): Future[Boolean] =
    collection.flatMap {
      _.insert(ordered = true)
        .one(discordPlayerLogged)
        .map(_.ok)
    }

  override def load(
      discordID: DiscordID
  ): Future[Option[DiscordPlayerLogged]] = {
    import models.ModelsJsonImplicits._
    collection.flatMap {
      _.find(
        Json.obj("discordID" -> discordID),
        Option.empty[DiscordPlayerLogged]
      ).one[DiscordPlayerLogged]
    }
  }

  override def find(query: String): Future[List[DiscordPlayerLogged]] = {
    def queryOnField(
        js: JSONCollection
    )(field: String): Future[List[DiscordPlayerLogged]] =
      js.find(
          Json.obj(
            field -> Json.obj("$regex" -> JsString(s"/.$query./i"))
          ),
          Option.empty[DiscordPlayerLogged]
        )
        .cursor[DiscordPlayerLogged]()
        .collect[List](5, Cursor.FailOnError[List[DiscordPlayerLogged]]())

    for {
      js <- collection
      (byName, byDiscriminator) <-
        queryOnField(js)("username").zip(queryOnField(js)("discriminator"))
    } yield {
      (byName ::: byDiscriminator).distinctBy(_.discordID)
    }
  }
}
