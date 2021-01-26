package models.daos

import javax.inject.Inject
import models.{DiscordUserData, DiscordUserHistory, DiscordUserLog, GuildID}
import org.joda.time.DateTime
import play.api.libs.json.Json
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.Cursor

import scala.concurrent.ExecutionContext.Implicits.global
import reactivemongo.play.json.collection.JSONCollection
import shared.models.DiscordID

import scala.concurrent.Future
class UserHistoryDAOImpl @Inject() (val reactiveMongoApi: ReactiveMongoApi)
    extends UserHistoryDAO {
  def collection: Future[JSONCollection] =
    reactiveMongoApi.database.map(_.collection("dsl.userhistory"))

  override def load(
      discordID: DiscordID
  ): Future[Option[DiscordUserHistory]] = {
    import models.ModelsJsonImplicits._
    val query = Json.obj("discordID" -> discordID)
    collection.flatMap(
      _.find(query, Option.empty[DiscordUserHistory]).one[DiscordUserHistory]
    )
  }

  override def updateWithLastInformation(
      discordID: DiscordID,
      guildID: GuildID,
      data: DiscordUserData
  ): Future[Boolean] = {
    for {
      alreadyRegistered <- load(discordID)
      result <- alreadyRegistered match {
        case Some(history) =>
          if (history.lastUserName.equals(data.userName))
            Future.successful(true)
          else
            updateLastUserName(discordID, guildID, data)
        case None =>
          register(discordID, guildID, data)
      }
    } yield {
      result
    }
  }

  protected override def register(
      discordID: DiscordID,
      guildID: GuildID,
      data: DiscordUserData
  ): Future[Boolean] = {
    collection.flatMap(
      _.insert(ordered = true)
        .one(
          DiscordUserHistory(
            discordID,
            data.discriminator,
            data.userName,
            Seq(
              DiscordUserLog(
                data.userName,
                guildID,
                data.avatarURL,
                DateTime.now()
              )
            )
          )
        )
        .map(_.ok)
    )

  }
  override protected def updateLastUserName(
      discordID: DiscordID,
      guildID: GuildID,
      data: DiscordUserData
  ): Future[Boolean] = {
    import models.ModelsJsonImplicits._
    collection
      .flatMap(
        _.update(ordered = true).one(
          Json.obj("discordID" -> discordID),
          Json.obj(
            "$push" -> Json.obj(
              "logs" -> DiscordUserLog(
                data.userName,
                guildID,
                data.avatarURL,
                DateTime.now()
              )
            ),
            "$set" -> Json.obj("lastUserName" -> data.userName)
          ),
          upsert = true
        )
      )
      .map(_.ok)
  }

  override def all(): Future[Seq[DiscordUserHistory]] =
    collection.flatMap(
      _.find(Json.obj(), Option.empty[DiscordUserHistory])
        .cursor[DiscordUserHistory]()
        .collect[List](-1, Cursor.FailOnError[List[DiscordUserHistory]]())
    )
}
