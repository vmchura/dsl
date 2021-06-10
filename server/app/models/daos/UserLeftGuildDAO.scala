package models.daos

import models.UserLeftGuild

import scala.concurrent.Future

trait UserLeftGuildDAO {
  def userLeft(leaving: UserLeftGuild): Future[Unit]
  def userReturned(returning: UserLeftGuild): Future[Unit]
  def userIsGone(test: UserLeftGuild): Future[Boolean]
}
