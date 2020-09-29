package models.services

import java.util.UUID

import scala.concurrent.ExecutionContext.Implicits.global
import javax.inject.Inject
import jobs.ReplayService
import models.{UserSmurf, ValidUserSmurf}
import models.daos.{UserSmurfDAO, ValidUserSmurfDAO}
import models.services.SmurfService.SmurfAdditionResult.{Added, AdditionResult, AlreadyRegistered, CantBeAdded}

import scala.concurrent.Future

class SmurfServiceImpl @Inject() (smurfDAO: UserSmurfDAO, replayService: ReplayService, validSmurfDAO: ValidUserSmurfDAO) extends SmurfService {
  import jobs._
  sealed trait SmurfError extends JobError
  case class MatchIDNotFound(matchID: UUID) extends SmurfError
  case class MatchIDNotFoundOnList(matchID: UUID) extends SmurfError

  override def acceptSmurf(discordUserID: String, matchID: UUID): Future[Boolean] = {
        for{
          userFound <- smurfDAO.findUser(discordUserID)
          user <- userFound.withFailure(MatchIDNotFound(matchID))
          matchSmurf <- user.notCheckedSmurf.find(_.resultID == matchID).withFailure(MatchIDNotFoundOnList(matchID))
          matchsWithSameSmurf <- Future.sequence(user.notCheckedSmurf.filter(_.smurf.equals(matchSmurf.smurf)).map(ms => smurfDAO.acceptNotCheckedSmurf(user.discordUser.discordID,ms)))
        }yield{
          matchsWithSameSmurf.forall(p => p)
        }

  }

  override def declineSmurf(discordUserID: String, matchID: UUID): Future[Boolean] = {
    for{
      userFound <- smurfDAO.findUser(discordUserID)
      user <- userFound.withFailure(MatchIDNotFound(matchID))
      matchSmurf <- user.notCheckedSmurf.find(_.resultID == matchID).withFailure(MatchIDNotFoundOnList(matchID))
      deleteReplays <- Future.sequence(user.notCheckedSmurf.filter(_.smurf.equals(matchSmurf.smurf)).map(ms => replayService.disableReplay(ms.resultID)))
      deleteSmurfs <- Future.sequence(user.notCheckedSmurf.filter(_.smurf.equals(matchSmurf.smurf)).map(ms => smurfDAO.declineNotCheckedSmurf(user.discordUser.discordID,ms)))
    }yield{

      deleteReplays.forall{
        case Right(x) => x
        case _ => false
      } && deleteSmurfs.forall(q => q)
    }

  }

  override def showAcceptedSmurfs(): Future[Seq[UserSmurf]] = smurfDAO.findUsersWithSmurfs()

  override def addSmurf(discordID: models.DiscordID, smurf: models.Smurf): Future[AdditionResult] = {
    for{
      exists <- validSmurfDAO.findOwner(smurf)
      result <- exists match {
        case Some(id) => Future.successful(if(id==discordID) AlreadyRegistered else CantBeAdded)
        case None => validSmurfDAO.add(discordID, smurf).map(b => if(b) Added else CantBeAdded)
      }
    }yield{
      result
    }

  }

  override def loadValidSmurfs(): Future[Seq[ValidUserSmurf]] = validSmurfDAO.all()

  override def loadSmurfs(discordID: models.DiscordID): Future[ValidUserSmurf] = validSmurfDAO.load(discordID)
}
