package models.daos

import models.{AuthToken, ModelsJsonImplicits, UserLeftGuild}
import reactivemongo.play.json.collection.JSONCollection

import javax.inject._
import play.api.libs.json._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import reactivemongo.api._
import reactivemongo.play.json._
import collection._
import play.api.Configuration
import play.modules.reactivemongo._
import reactivemongo.play.json.collection.JSONCollection
import ModelsJsonImplicits._
import scala.concurrent.Future

class UserLeftGuildDAOImpl @Inject() (val reactiveMongoApi: ReactiveMongoApi)
    extends UserLeftGuildDAO {
  def collection: Future[JSONCollection] =
    reactiveMongoApi.database.map(_.collection("dsl.userleftguild"))
  override def userLeft(leaving: UserLeftGuild): Future[Unit] =
    collection.flatMap {
      _.insert(ordered = true)
        .one(leaving)
        .map(_.ok)
    }

  override def userReturned(returning: UserLeftGuild): Future[Unit] = {
    for {
      present <- userIsGone(returning)
      _ <-
        if (present)
          collection.flatMap(
            _.delete(ordered = true).one(returning)
          )
        else Future.successful(())
    } yield {
      ()
    }
  }

  override def userIsGone(test: UserLeftGuild): Future[Boolean] =
    collection
      .flatMap(
        _.find(
          test,
          Option.empty[UserLeftGuild]
        ).one[UserLeftGuild]
      )
      .map(_.nonEmpty)
}
