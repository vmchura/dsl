package jobs
import javax.inject.Inject
import models.services.{ChallongeTournamentService, DiscordUserService, ParticipantsService, TournamentService}
import models.{DiscordUser, Match, Participant, Tournament, User}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class TournamentBuilder @Inject() (tournamentService: TournamentService,
                                   participantsService: ParticipantsService,
                                   challongeTournamentService: ChallongeTournamentService,
                                   discordUserService: DiscordUserService) {
  implicit class opt2Future[A](opt: Option[A]) {
    def withFailure(f: TournamentBuilderError): Future[A] = opt match {
      case None => Future.failed(f)
      case Some(x) => Future.successful(x)
    }
  }
  implicit class flag2Future(flag: Boolean){
    def withFailure(f: TournamentBuilderError): Future[Boolean] = if(flag) Future.successful(true) else Future.failed(f)
  }

  def convertToEither[A](f: Future[A]): Future[Either[TournamentBuilderError,A]] = {
    f.map(v => Right(v)).recoverWith{
      case exception: TournamentBuilderError => Future.successful(Left(exception))
      case exception => Future.successful(Left(UnknowError(exception.toString)))
    }
  }

  def buildTournament(discordID: String, challongeID: String): Future[Either[TournamentBuilderError,Tournament]] = {


    val tournamentCreation = for {
      challongeTournamentOpt <- challongeTournamentService.findChallongeTournament(discordID)(challongeID)
      challongeTournament <- challongeTournamentOpt.withFailure(CannontAccessChallongeTournament(challongeID))
      discordUsersOpt <- discordUserService.findMembersOnGuild(discordID)
      _ <- discordUsersOpt.withFailure(CannotAccesDiscordGuild(discordID))
      tournamentInsertion <- tournamentService.saveTournament(challongeTournament.tournament)
      _ <- tournamentInsertion.withFailure(TournamentAlreadyCreated(challongeID))
      participantInsertion <- Future.sequence(challongeTournament.participants.map(participantsService.saveParticipant))
      _ <- Future.sequence(participantInsertion.zip(challongeTournament.participants).map{case (f,p) => f.withFailure(CannotAddSomeParticipant(p.toString))})
    }yield{
      challongeTournament.tournament
    }

    convertToEither(tournamentCreation)
  }
  def getParticipantsUsers(challongeTournamentID: Long): Future[Either[TournamentBuilderError, (Tournament,Seq[Participant],Seq[DiscordUser])]] = {
    val tournamentCreation = for {
      tournamentFromDBOpt <- tournamentService.loadTournament(challongeTournamentID)
      tournamentDB <- tournamentFromDBOpt.withFailure(TournamentNotBuild(challongeTournamentID))
      challongeTournamentOpt <- challongeTournamentService.findChallongeTournament(tournamentDB.discordServerID)(tournamentDB.urlID)
      challongeTournament <- challongeTournamentOpt.withFailure(CannontAccessChallongeTournament(tournamentDB.urlID))
      discordUsersOpt <- discordUserService.findMembersOnGuild(tournamentDB.discordServerID)
      discordUsers <- discordUsersOpt.withFailure(CannotAccesDiscordGuild(tournamentDB.discordServerID))
    }yield{
      (challongeTournament.tournament,challongeTournament.participants,discordUsers)
    }

    convertToEither(tournamentCreation)
  }

  def getMatches(challongeTournamentID: Long, userOpt: Option[User]): Future[Either[TournamentBuilderError,Seq[Match]]] = {
    val tournamentCreation = for {
      tournamentFromDBOpt <- tournamentService.loadTournament(challongeTournamentID)
      tournamentDB <- tournamentFromDBOpt.withFailure(TournamentNotBuild(challongeTournamentID))
      challongeTournamentOpt <- challongeTournamentService.findChallongeTournament(tournamentDB.discordServerID)(tournamentDB.urlID)
      challongeTournament <- challongeTournamentOpt.withFailure(CannontAccessChallongeTournament(tournamentDB.urlID))
      participants <- userOpt.fold(Future.successful(Seq.empty[Participant]))(user => participantsService.loadParticipantByDiscordUserID(user.loginInfo.providerKey))
    }yield{
      println(participants)
      println(userOpt)
      userOpt.fold(challongeTournament.matches)(_ =>
        challongeTournament.matches.filter(m => participants.exists(p => p.participantPK.chaNameID == m.firstChaNameID || p.participantPK.chaNameID == m.secondChaNameID)))

    }

    convertToEither(tournamentCreation)
  }
}
