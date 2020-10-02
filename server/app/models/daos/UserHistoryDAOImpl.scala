package models.daos

import javax.inject.Inject
import models.{DiscordDiscriminator, DiscordID, DiscordUserHistory, DiscordUserLog}
import org.joda.time.DateTime
import play.api.libs.json.Json
import play.modules.reactivemongo.ReactiveMongoApi

import scala.concurrent.ExecutionContext.Implicits.global
import reactivemongo.play.json.collection.JSONCollection

import scala.concurrent.Future
class UserHistoryDAOImpl @Inject() (val reactiveMongoApi: ReactiveMongoApi) extends UserHistoryDAO {
  def collection: Future[JSONCollection] = reactiveMongoApi.database.map(_.collection("dsl.userhistory"))


  override def load(discordID: models.DiscordID): Future[Option[DiscordUserHistory]] = {
    val query = Json.obj("discordID" -> discordID)
    collection.flatMap(_.find(query,Option.empty[DiscordUserHistory]).one[DiscordUserHistory])
  }

  override def updateWithLastInformation(discordID: models.DiscordID, discriminator: DiscordDiscriminator,userLog: String): Future[Boolean] = {
    for{
      alreadyRegistered <- load(discordID)
      result <- alreadyRegistered match {
        case Some(history) =>
          if(history.lastUserName.equals(userLog))
            Future.successful(true)
          else
            updateLastUserName(discordID,userLog)
        case None =>
          register(discordID,discriminator,userLog)
      }
    }yield{
      result
    }
  }

  protected override def register(discordID: DiscordID, discriminator: DiscordDiscriminator, userLog: String): Future[Boolean] = {
    collection.
      flatMap(_.insert(ordered=true).
        one(DiscordUserHistory(discordID,discriminator,userLog,Seq(DiscordUserLog(userLog,DateTime.now())))).
      map(_.ok))

  }
  override protected def updateLastUserName(discordID: DiscordID, userLog: String): Future[Boolean] = {
    collection.
      flatMap(_.update(ordered=true).
        one(Json.obj("discordID" -> discordID),
          Json.obj(
            "$push" ->  Json.obj("logs"         -> DiscordUserLog(userLog,DateTime.now())),
                   "$set" ->  Json.obj("lastUserName" -> userLog)), upsert = true)).
      map(_.ok)
  }
}
