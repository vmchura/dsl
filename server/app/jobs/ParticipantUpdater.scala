package jobs

import javax.inject.Inject
import models.daos.UserGuildDAO
import models.{DiscordID, GuildID, Participant, ParticipantPK}
import models.services.{ParticipantsService, TournamentService}
import shared.utils.BasicComparableByLabel
import upickle.default._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ParticipantUpdater @Inject() (participantsService: ParticipantsService, tournamentService: TournamentService, userGuildDAO: UserGuildDAO) {
  def updateParticipant(first: BasicComparableByLabel, second: BasicComparableByLabel): Future[Either[ParticipantUpdaterError,Participant]] = {
    implicit class opt2Future[A](opt: Option[A]) {
      def withFailure(f: ParticipantUpdaterError): Future[A] = opt match {
        case None => Future.failed(f)
        case Some(x) => Future.successful(x)
      }
    }
    implicit class flag2Future(flag: Boolean){
      def withFailure(f: ParticipantUpdaterError): Future[Boolean] = if(flag) Future.successful(true) else Future.failed(f)
    }


    val updateProcedure = for{
      participantPK <- try{ Future.successful(read[ParticipantPK](first.id))} catch {case _: Throwable => Future.failed(ParticipantPKBadFormed(first.id))}
      discordUserID <- try{ Future.successful(read[String](second.id))} catch {case _: Throwable => Future.failed(UnknowParticipantUpdateError(s"second parameter id is not valid: $second"))}
      oldParticipantOpt <- participantsService.loadParticipant(participantPK)
      oldParticipant <- oldParticipantOpt.withFailure(ParticipantNotFound(participantPK))
      newParticipant <- Future.successful(oldParticipant.copy(discordUserID = Some(discordUserID)))
      tournamentOpt <- tournamentService.loadTournament(oldParticipant.participantPK.challongeID)
      tournament <- tournamentOpt.withFailure(TournamentNotRelated(participantPK))
      guildUpdated <- userGuildDAO.addGuildToUser(DiscordID(discordUserID),GuildID(tournament.discordServerID))
      _ <- guildUpdated.withFailure(GuildNotUpdated(participantPK))
      update <- participantsService.updateParticipantRelation(newParticipant)
      _ <- update.withFailure(ParticipantCantUpdate(participantPK))
    }yield{

      newParticipant
    }

    updateProcedure.map(x => Right(x)).recoverWith{
      case error: ParticipantUpdaterError  => Future.successful(Left(error))
      case error => Future.successful(Left(UnknowParticipantUpdateError(error.toString)))
    }

  }
}
