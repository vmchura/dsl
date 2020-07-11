package jobs

import javax.inject.Inject
import models.services.{ChallongeTournamentService, DiscordUserService, ParticipantsService, TournamentService}
import models.{DiscordUser, Participant, Tournament}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class TournamentBuilder @Inject() (tournamentService: TournamentService,
                                   participantsService: ParticipantsService,
                                   challongeTournamentService: ChallongeTournamentService,
                                   discordUserService: DiscordUserService) {
  def buildTournament(discordID: String, challongeID: String): Future[Either[TournamentBuilderError,(Tournament,Seq[Participant],Seq[DiscordUser])]] = {
    implicit class opt2Future[A](opt: Option[A]) {
      def withFailure(f: TournamentBuilderError): Future[A] = opt match {
        case None => Future.failed(f)
        case Some(x) => Future.successful(x)
      }
    }
    implicit class flag2Future(flag: Boolean){
      def withFailure(f: TournamentBuilderError): Future[Boolean] = if(flag) Future.successful(true) else Future.failed(f)
    }

    val tournamentCreation = for {
      challongeTournamentOpt <- challongeTournamentService.findChallongeTournament(discordID)(challongeID)
      challongeTournament <- challongeTournamentOpt.withFailure(CannontAccessChallongeTournament(challongeID))
      discordUsersOpt <- discordUserService.findMembersOnGuild(discordID)
      discordUsers <- discordUsersOpt.withFailure(CannotAccesDiscordGuild(discordID))
      tournamentInsertion <- tournamentService.saveTournament(challongeTournament.tournament)
      _ <- tournamentInsertion.withFailure(TournamentAlreadyCreated(challongeID))
      participantInsertion <- Future.sequence(challongeTournament.participants.map(participantsService.saveParticipant))
      _ <- Future.sequence(participantInsertion.zip(challongeTournament.participants).map{case (f,p) => f.withFailure(CannotAddSomeParticipant(p.toString))})
    }yield{
      (challongeTournament.tournament,challongeTournament.participants,discordUsers)
    }

    tournamentCreation.map(v => Right(v)).recoverWith{
        case exception: TournamentBuilderError => Future.successful(Left(exception))
        case exception => Future.successful(Left(UnknowError(exception.toString)))
    }




  }
}
