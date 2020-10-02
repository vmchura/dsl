package models.daos


import javax.inject._
import models._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.libs.json.Json
import play.modules.reactivemongo._
import reactivemongo.api.Cursor
import reactivemongo.play.json.collection.JSONCollection
import reactivemongo.play.json.compat._
class UserGuildDAOImpl @Inject() (val reactiveMongoApi: ReactiveMongoApi) extends UserGuildDAO {

  def collection: Future[JSONCollection] = reactiveMongoApi.database.map(_.collection("dsl.userguild"))

  private def find(discordID: DiscordID): Future[Option[UserGuild]] = {
    val query = Json.obj("discordID" -> discordID)
    collection.flatMap(_.find(query,Option.empty[UserGuild]).one[UserGuild])
  }
  private def save(userGuild: UserGuild): Future[Boolean] = collection.flatMap(_.insert(ordered=true).one(userGuild)).map(_.ok)

  private def push(discordID: DiscordID, guildID: GuildID): Future[Boolean] = collection.
    flatMap(_.update(ordered=true).
      one(Json.obj("discordID" -> discordID), Json.obj("$push" -> Json.obj("guilds"-> guildID)), upsert = true)).
    map(_.ok)

  override def load(discordID: DiscordID): Future[Set[GuildID]] = {
    find(discordID).map{
      case Some(result) => result.guilds
      case None =>Set.empty[GuildID]
    }
  }

  override def addGuildToUser(discordID: DiscordID, guildID: GuildID): Future[Boolean] = {
    find(discordID).flatMap{
      case Some(result) => if(result.guilds.contains(guildID)) Future.successful(true) else push(discordID, guildID)
      case None => save(UserGuild(discordID,Set(guildID)))
    }
  }

  override def all(): Future[Seq[UserGuild]] = {
    collection.flatMap(_.find(Json.obj(),Option.empty[UserGuild]).cursor[UserGuild]().collect[List](-1,Cursor.FailOnError[List[UserGuild]]()))
  }
}
