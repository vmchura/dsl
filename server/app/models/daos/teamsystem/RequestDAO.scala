package models.daos.teamsystem

import models.teamsystem.{RequestJoin, RequestJoinID, TeamID}

import scala.concurrent.Future

trait RequestDAO {
  def loadRequest(requestID: RequestJoinID): Future[Option[RequestJoin]]
  def requestsToTeam(teamID: TeamID): Future[Seq[RequestJoin]]
  def addRequest(request: RequestJoin): Future[RequestJoinID]
  def removeRequest(requestID: RequestJoinID): Future[Boolean]
}
