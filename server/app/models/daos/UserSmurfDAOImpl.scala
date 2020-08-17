package models.daos


import javax.inject.Inject
import models.{DiscordUser, MatchSmurf, UserSmurf}
import play.modules.reactivemongo.ReactiveMongoApi

import scala.concurrent.ExecutionContext.Implicits.global
import reactivemongo.play.json.collection.JSONCollection
import play.api.libs.json.{JsObject, Json}
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
      user <- findUser(discordUser.discordID)
      insertion <- user.fold(collection.
        flatMap(_.insert(ordered=true).
          one(UserSmurf(discordUser,Nil,Nil))).
        map(_.ok))(_ => {
        Future.successful(true)

      })
    }yield{
      insertion
    }
  }

  override def findUser(discordUserID: String): Future[Option[UserSmurf]] = {
    val query = Json.obj("discordUser.discordID" -> discordUserID)
    collection.flatMap(_.find(query,Option.empty[UserSmurf]).one[UserSmurf])

  }
  override def removeUser(discordUserID: String): Future[Boolean] =  {
    collection.
      flatMap(_.delete(ordered = true).one(Json.obj("discordUser.discordID" -> discordUserID))).map(_.ok)
  }

  override def addNotCheckedSmurf(discordUserID: String, newSmurf: MatchSmurf): Future[Boolean] = collection.
    flatMap(_.update(ordered=true).
      one(Json.obj("discordUser.discordID" -> discordUserID), Json.obj("$push" -> Json.obj("notCheckedSmurf"-> newSmurf)), upsert = true)).
    map(_.ok)

  override def acceptNotCheckedSmurf(discordUserID: String, smurfToRemove: MatchSmurf): Future[Boolean] = {
    for {
     removed <-  declineNotCheckedSmurf(discordUserID: String, smurfToRemove: MatchSmurf)
      added <- if(removed) addSmurf(discordUserID,smurfToRemove) else Future.successful(false)
    }yield{
      added
    }
  }

  private def findSequenceUserSmurf(query: JsObject): Future[Seq[UserSmurf]] = {
    collection.flatMap(_.find(query,Option.empty[UserSmurf]).cursor[UserSmurf]().collect[List](-1,Cursor.FailOnError[List[UserSmurf]]()))
  }
  override def findUsersNotCompletelyDefined(): Future[Seq[UserSmurf]] = {
    val query: JsObject = Json.obj("notCheckedSmurf"-> Json.obj("$exists" -> true, "$ne" -> Json.arr()))
    findSequenceUserSmurf(query)

  }

  override def findUsers(discordUsersID: Seq[String]): Future[Seq[UserSmurf]] = {
    val query = Json.obj("discordUser.discordID" -> Json.obj("$in" -> discordUsersID))
    findSequenceUserSmurf(query)

  }

  override def declineNotCheckedSmurf(discordUserID: String, smurfToRemove: MatchSmurf): Future[Boolean] = {
    collection.
      flatMap(_.update(ordered = true).
        one(Json.obj("discordUser.discordID" -> discordUserID), Json.obj("$pull" -> Json.obj("notCheckedSmurf" -> smurfToRemove)), upsert = true)).
      map(_.ok)
  }

  override def findUsersWithSmurfs(): Future[Seq[UserSmurf]] = {
    val query: JsObject = Json.obj("matchSmurf"-> Json.obj("$exists" -> true, "$ne" -> Json.arr()))
    findSequenceUserSmurf(query)

  }
}
