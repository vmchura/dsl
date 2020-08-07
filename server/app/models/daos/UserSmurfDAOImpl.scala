package models.daos

import javax.inject.Inject
import models.{DiscordUser, MatchSmurf, UserSmurf}
import play.modules.reactivemongo.ReactiveMongoApi

import scala.concurrent.ExecutionContext.Implicits.global
import reactivemongo.play.json.collection.JSONCollection
import play.api.libs.json.Json
import reactivemongo.api.Cursor

import scala.concurrent.Future
class UserSmurfDAOImpl @Inject() (val reactiveMongoApi: ReactiveMongoApi) extends UserSmurfDAO {
  def collection: Future[JSONCollection] = reactiveMongoApi.database.map(_.collection("dsl.usersmurf"))

  override def findBySmurf(smurf: String): Future[List[UserSmurf]] = {
    collection.flatMap(_.find(Json.obj("matchSmurf.smurf"-> smurf),Option.empty[UserSmurf]).cursor[UserSmurf]().collect[List](-1,Cursor.FailOnError[List[UserSmurf]]()))
  }
  override def addSmurf(discordUserID: String, newSmurf: MatchSmurf): Future[Boolean] = {
    collection.
      flatMap(_.update(ordered=true).
      one(Json.obj("discordUser.discordID" -> discordUserID), Json.obj("$push" -> Json.obj("matchSmurf"-> newSmurf)), upsert = true)).
      map(_.ok)
  }
  override def removeSmurf(discordUserID : String, smurfToRemove: MatchSmurf): Future[Boolean] = {
    collection.
      flatMap(_.update(ordered=true).
      one(Json.obj("discordUser.discordID" -> discordUserID), Json.obj("$pull" -> Json.obj("matchSmurf"-> smurfToRemove)), upsert = true)).
      map(_.ok)
  }


  override def addUser(discordUser: DiscordUser): Future[Boolean] = {
    for{
      user <- getUserSmurf(discordUser.discordID)
      insertion <- user.fold(collection.
        flatMap(_.insert(ordered=true).
          one(UserSmurf(discordUser,Nil))).
        map(_.ok))(_ => {
        Future.successful(true)

      })
    }yield{
      insertion
    }
  }

  override def getUserSmurf(discordUserID: String): Future[Option[UserSmurf]] = {
    val query = Json.obj("discordUser.discordID" -> discordUserID)
    collection.flatMap(_.find(query,Option.empty[UserSmurf]).one[UserSmurf])

  }
  override def removeUser(discordUserID: String): Future[Boolean] =  {
    collection.
      flatMap(_.delete(ordered = true).one(Json.obj("discordUser.discordID" -> discordUserID))).map(_.ok)
  }
}
