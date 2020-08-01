package jobs
import javax.inject.Inject
import models.daos.ReplayMatchDAO
import models.services.{ChallongeTournamentService, DiscordUserService, ParticipantsService, TournamentService}
import models.{DiscordUser, Match, MatchDiscord, Participant, TWithReplays, Tournament, User}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class TournamentBuilder @Inject() (tournamentService: TournamentService,
                                   participantsService: ParticipantsService,
                                   challongeTournamentService: ChallongeTournamentService,
                                   discordUserService: DiscordUserService,
                                   replayMatchDAO: ReplayMatchDAO) {

  def formFuture[A](f: Future[A]): Future[Either[JobError,A]] = {
    convertToEither(s => UnknowTournamentBuilderError(s))(f)

  }


  def buildTournament(discordID: String, challongeID: String): Future[Either[JobError,Tournament]] = {


    val tournamentCreation = for {
      challongeTournamentOpt  <- challongeTournamentService.findChallongeTournament(discordID)(challongeID)
      challongeTournament     <- challongeTournamentOpt.withFailure(CannontAccessChallongeTournament(challongeID))
      discordUsersOpt         <- discordUserService.findMembersOnGuild(discordID)
      _                       <- discordUsersOpt.withFailure(CannotAccesDiscordGuild(discordID))
      tournamentInsertion     <- tournamentService.saveTournament(challongeTournament.tournament)
      _                       <- tournamentInsertion.withFailure(TournamentAlreadyCreated(challongeID))
      participantInsertion    <- Future.sequence(challongeTournament.participants.map(participantsService.saveParticipant))
      _                       <- Future.sequence(participantInsertion.zip(challongeTournament.participants).map{case (f,p) => f.withFailure(CannotAddSomeParticipant(p.toString))})
    }yield{
      challongeTournament.tournament
    }

    formFuture(tournamentCreation)
  }
  def getParticipantsUsers(challongeTournamentID: Long): Future[Either[JobError, (Tournament,Seq[Participant],Seq[DiscordUser])]] = {
    val tournamentCreation = for {
      tournamentFromDBOpt       <- tournamentService.loadTournament(challongeTournamentID)
      tournamentDB              <- tournamentFromDBOpt.withFailure(TournamentNotBuild(challongeTournamentID))
      challongeTournamentOpt    <- challongeTournamentService.findChallongeTournament(tournamentDB.discordServerID)(tournamentDB.urlID)
      challongeTournament       <- challongeTournamentOpt.withFailure(CannontAccessChallongeTournament(tournamentDB.urlID))
      discordUsersOpt           <- discordUserService.findMembersOnGuild(tournamentDB.discordServerID)
      discordUsers              <- discordUsersOpt.withFailure(CannotAccesDiscordGuild(tournamentDB.discordServerID))
    }yield{
      (challongeTournament.tournament,challongeTournament.participants,discordUsers)
    }

    formFuture(tournamentCreation)
  }

  def getMatches(challongeTournamentID: Long, userOpt: Option[User]): Future[Either[JobError,Seq[Match]]] = {
    val matchesFromChallonge = for {
      tournamentFromDBOpt <- tournamentService.loadTournament(challongeTournamentID)
      tournamentDB <- tournamentFromDBOpt.withFailure(TournamentNotBuild(challongeTournamentID))
      challongeTournamentOpt <- challongeTournamentService.findChallongeTournament(tournamentDB.discordServerID)(tournamentDB.urlID)
      challongeTournament <- challongeTournamentOpt.withFailure(CannontAccessChallongeTournament(tournamentDB.urlID))
      participants <- userOpt.fold(Future.successful(Seq.empty[Participant]))(user => participantsService.loadParticipantByDiscordUserID(user.loginInfo.providerKey))
    }yield{
      userOpt.fold(challongeTournament.matches)(_ =>
        challongeTournament.matches.filter(m => participants.exists(p => p.participantPK.chaNameID == m.firstChaNameID || p.participantPK.chaNameID == m.secondChaNameID)))

    }
    val replaysAttached = attachReplays(challongeTournamentID,matchesFromChallonge)

    formFuture(replaysAttached)
  }

  def attachReplays[A <: TWithReplays[A]](challongeTournamentID: Long,replaysContainer: Future[Seq[A]]): Future[Seq[A]] = {
    for{
      matches <- replaysContainer
      replaysForTournament <- replayMatchDAO.loadAllByTournament(challongeTournamentID)
    }yield{
      matches.map{ m =>
        m.withReplays(replaysForTournament.filter(_.matchID == m.matchPK.challongeMatchID))

      }
    }
  }
  def getMatchesDiscord(challongeTournamentID: Long, userOpt: Option[User]): Future[Either[JobError,Seq[MatchDiscord]]] = {
    def filterByUser(md: MatchDiscord): Boolean = userOpt.fold(true)(u => u.loginInfo.providerKey.equals(md.discord1ID) || u.loginInfo.providerKey.equals(md.discord2ID))


    val matchesFromChallonge: Future[Seq[MatchDiscord]] = for {
      tournamentFromDBOpt <- tournamentService.loadTournament(challongeTournamentID)
      tournamentDB <- tournamentFromDBOpt.withFailure(TournamentNotBuild(challongeTournamentID))
      challongeTournamentOpt <- challongeTournamentService.findChallongeTournament(tournamentDB.discordServerID)(tournamentDB.urlID)
      challongeTournament <- challongeTournamentOpt.withFailure(CannontAccessChallongeTournament(tournamentDB.urlID))
      participants <- participantsService.loadParticipantDefinedByTournamentID(challongeTournamentID)
    }yield{


      val matchesDefined = challongeTournament.matches.flatMap{ m =>
        for{
          p1 <- participants.find(_.participantPK.chaNameID == m.firstChaNameID)
          p2 <- participants.find(_.participantPK.chaNameID == m.secondChaNameID)
        }yield{
          MatchDiscord(m.matchPK,m.round, m.firstChaNameID, m.secondChaNameID,p1.discordUserID,p2.discordUserID, p1.chaname, p2.chaname)
        }

      }
      matchesDefined.filter(filterByUser)

    }
    val replaysAttached = attachReplays(challongeTournamentID,matchesFromChallonge)

    formFuture(replaysAttached)
  }


}
