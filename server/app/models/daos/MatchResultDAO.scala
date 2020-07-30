package models.daos

import java.util.UUID

import models.{MatchResult, ReplayRecord}

import scala.concurrent.Future

trait MatchResultDAO {

  def save(matchResultRecord: MatchResult): Future[Boolean]
  def find(matchResultID: UUID): Future[Option[MatchResult]]
}
