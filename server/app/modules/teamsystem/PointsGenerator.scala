package modules.teamsystem

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import com.google.inject.Provides
import models.Smurf
import models.daos.teamsystem.{PointsDAO, TeamDAO, TeamReplayDAO}
import models.teamsystem.{Points, Team, TeamID}
import modules.teamsystem.UniqueSmurfWatcher
import play.api.libs.concurrent.ActorModule
import shared.models.{DiscordID, DiscordPlayerLogged, ReplayTeamID}
import shared.models.StarCraftModels.OneVsOne

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Success

object PointsGenerator extends ActorModule {
  sealed trait InternalCommand
  sealed trait Command extends InternalCommand
  override type Message = Command

  case class ProcessPoints(
      replayTeamID: ReplayTeamID,
      uploader: DiscordID,
      oneVsOne: OneVsOne
  ) extends Command
  case class ProcessPointsFromDB(
      replayTeamID: ReplayTeamID
  ) extends Command
  case class DiscordPlayerFound(user: DiscordPlayerLogged)
      extends InternalCommand
  case class DiscordPlayerAbsent() extends InternalCommand
  case class ReplayReadyToProcess(
      replayTeamID: ReplayTeamID,
      oneVsOne: OneVsOne,
      uploader: DiscordID
  ) extends InternalCommand

  case class WithTeam(teamID: TeamID) extends InternalCommand
  case class NoTeam() extends InternalCommand
  case object IgnoreCommand extends InternalCommand

  private def messageSmurfAdapter(ctx: ActorContext[InternalCommand]) =
    ctx.messageAdapter[UniqueSmurfWatcher.Response] {
      case UniqueSmurfWatcher
            .UserOwner(discordPlayerLogged) =>
        DiscordPlayerFound(discordPlayerLogged)
      case UniqueSmurfWatcher.SmurfNotAssigned() =>
        DiscordPlayerAbsent()
      case UniqueSmurfWatcher.ErrorSmurfWatcher(_) =>
        IgnoreCommand
    }

  @Provides
  def apply(
      pointsDAO: PointsDAO,
      uniqueSmurfWatcher: ActorRef[UniqueSmurfWatcher.Command],
      teamDAO: TeamDAO,
      teamReplayDAO: TeamReplayDAO
  ): Behavior[Command] = {

    def pipeMessageIfInTeam(ctx: ActorContext[InternalCommand])(
        discordID: DiscordID
    )(onTeam: Team => InternalCommand) =
      ctx.pipeToSelf(
        teamDAO
          .teamsOf(discordID)
          .map(_.find(_.isOfficial(discordID)))
      ) {
        case Success(Some(team)) =>
          onTeam(team)
        case Success(None) => NoTeam()
        case _             => IgnoreCommand
      }

    def pipeLocateOwner(ctx: ActorContext[InternalCommand])(smurf: Smurf) =
      uniqueSmurfWatcher ! UniqueSmurfWatcher.LocateOwner(
        smurf,
        messageSmurfAdapter(ctx)
      )

    def processNextPoints(
        winner: Option[Option[(DiscordPlayerLogged, Option[TeamID])]],
        loser: Option[Option[(DiscordPlayerLogged, Option[TeamID])]]
    )(implicit
        replayTeamID: ReplayTeamID,
        oneVsOne: OneVsOne,
        uploader: DiscordID
    ): Behavior[InternalCommand] =
      Behaviors.setup { ctx =>
        def addPoints(
            reason: String,
            points: Int
        )(teamID: TeamID, user: DiscordPlayerLogged): Future[Boolean] = {
          pointsDAO.save(
            Points(
              teamID,
              replayTeamID,
              points,
              user.discordID,
              date = oneVsOne.startTime,
              reason,
              enabled = true
            )
          )
        }
        def addPointsByUploading(
            teamID: TeamID,
            user: DiscordPlayerLogged
        ): Future[Boolean] = addPoints("Por subir replay", 5)(teamID, user)
        def addPointsByPlaying(
            teamID: TeamID,
            user: DiscordPlayerLogged
        ): Future[Boolean] = addPoints("Por jugar", 5)(teamID, user)
        def addPointsByWinning(
            teamID: TeamID,
            user: DiscordPlayerLogged
        ): Future[Boolean] = addPoints("Por ganar", 15)(teamID, user)
        def addPointsAgainstAMember(
            teamID: TeamID,
            user: DiscordPlayerLogged
        ): Future[Boolean] =
          addPoints("Por jugar contra otro miembro", 5)(teamID, user)
        def addPointsByAgainstATeam(
            teamID: TeamID,
            user: DiscordPlayerLogged
        ): Future[Boolean] =
          addPoints("Por jugar contra otro equipo", 5)(teamID, user)

        def addPointsByGame(
            teamID: TeamID,
            winner: Option[DiscordPlayerLogged],
            loser: Option[DiscordPlayerLogged],
            oneVsOne: OneVsOne
        ): Future[Boolean] = {
          (winner, loser) match {
            case (Some(user), None) =>
              if (user.discordID == uploader)
                addPointsByUploading(teamID, user)
              addPointsByPlaying(teamID, user)
              addPointsByWinning(teamID, user)
            case (None, Some(user)) =>
              if (user.discordID == uploader)
                addPointsByUploading(teamID, user)
              addPointsByPlaying(teamID, user)
            case _ => Future.successful(true)
          }
        }

        Behaviors.receiveMessage { message =>
          (winner, message) match {
            case (_, IgnoreCommand) => Behaviors.stopped
            case (None, ReplayReadyToProcess(_, _, _)) =>
              pipeLocateOwner(ctx)(Smurf(oneVsOne.winner.smurf))
              processNextPoints(None, None)
            case (None, DiscordPlayerFound(user)) =>
              pipeMessageIfInTeam(ctx)(user.discordID)(team =>
                WithTeam(team.teamID)
              )
              processNextPoints(Some(Some((user, None))), None)
            case (None, DiscordPlayerAbsent()) =>
              pipeLocateOwner(ctx)(Smurf(oneVsOne.loser.smurf))
              processNextPoints(Some(None), None)
            case (Some(Some((winner, None))), WithTeam(teamID)) =>
              addPointsByGame(teamID, Some(winner), None, oneVsOne)
              pipeLocateOwner(ctx)(Smurf(oneVsOne.loser.smurf))
              processNextPoints(Some(Some((winner, Some(teamID)))), None)
            case (Some(Some((winner, None))), NoTeam()) =>
              pipeLocateOwner(ctx)(Smurf(oneVsOne.loser.smurf))
              processNextPoints(Some(Some((winner, None))), None)
            case (Some(None), DiscordPlayerFound(user)) =>
              pipeMessageIfInTeam(ctx)(user.discordID)(team =>
                WithTeam(team.teamID)
              )
              processNextPoints(Some(None), Some(Some((user, None))))
            case (Some(None), DiscordPlayerAbsent()) =>
              Behaviors.stopped
            case (Some(None), WithTeam(teamID)) =>
              loser.foreach(loserLogged =>
                addPointsByGame(teamID, None, loserLogged.map(_._1), oneVsOne)
              )

              Behaviors.stopped
            case (
                  winnerOpt @ Some(Some((winner, Some(teamID)))),
                  DiscordPlayerFound(user)
                ) =>
              addPointsAgainstAMember(teamID, winner)
              pipeMessageIfInTeam(ctx)(user.discordID)(team =>
                WithTeam(team.teamID)
              )
              processNextPoints(winnerOpt, Some(Some((user, None))))
            case (
                  winnerOpt @ Some(Some((_, None))),
                  DiscordPlayerFound(user)
                ) =>
              pipeMessageIfInTeam(ctx)(user.discordID)(team =>
                WithTeam(team.teamID)
              )
              processNextPoints(winnerOpt, Some(Some((user, None))))
            case (Some(_), DiscordPlayerAbsent()) =>
              Behaviors.stopped

            case (
                  Some(Some((winner, Some(winnerTeamID)))),
                  WithTeam(loserTeamID)
                ) =>
              addPointsByAgainstATeam(winnerTeamID, winner)
              loser.foreach {
                case Some((loserLogged, _)) =>
                  addPointsByGame(
                    loserTeamID,
                    None,
                    Some(loserLogged),
                    oneVsOne
                  )
                  addPointsAgainstAMember(loserTeamID, loserLogged)
                  addPointsByAgainstATeam(loserTeamID, loserLogged)
                case None =>
              }
              Behaviors.stopped
            case (x, y) =>
              println(s"Error $x,$y")
              Behaviors.stopped
          }

        }

      }

    Behaviors
      .setup[InternalCommand] { ctx =>
        Behaviors.receiveMessage { message =>
          message match {
            case ProcessPoints(replayTeamID, uploader, oneVsOne) =>
              ctx.pipeToSelf(pointsDAO.hasBeenProcessed(replayTeamID)) {
                case Success(false) =>
                  ReplayReadyToProcess(replayTeamID, oneVsOne, uploader)
                case _ => IgnoreCommand
              }
            case ProcessPointsFromDB(replayTeamID) =>
              ctx.pipeToSelf(teamReplayDAO.load(replayTeamID)) {
                case Success(Some(replayInfo)) =>
                  ProcessPoints(
                    replayTeamID,
                    replayInfo.senderID,
                    replayInfo.game
                  )
                case _ => IgnoreCommand
              }
            case initialState @ ReplayReadyToProcess(
                  replayTeamID,
                  oneVsOne,
                  uploader
                ) =>
              val worker = ctx.spawnAnonymous(
                processNextPoints(None, None)(replayTeamID, oneVsOne, uploader)
              )
              worker ! initialState

            case IgnoreCommand =>
            case _ =>
              throw new IllegalArgumentException("Error receiving message")
          }
          Behaviors.same
        }
      }
      .narrow
  }
}
