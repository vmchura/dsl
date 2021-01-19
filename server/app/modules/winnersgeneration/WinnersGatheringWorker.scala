package modules.winnersgeneration

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import models.{Tournament, TournamentSeries, UserGuild}
import modules.winnersgeneration.WinnersGathering.{
  GatheringComplete,
  GatheringSucess
}

object WinnersGatheringWorker {
  trait InternalCommand
  case class TournamentsGathered(tournaments: Seq[Tournament])
      extends InternalCommand
  case class TournamentSeriesGathered(tournamentSeries: Seq[TournamentSeries])
      extends InternalCommand
  case class UsersGathered(users: Seq[UserGuild]) extends InternalCommand

  case class GatheredInformationBuilder private (
      tournaments: Option[Seq[Tournament]],
      tournamentSeries: Option[Seq[TournamentSeries]],
      users: Option[Seq[UserGuild]]
  ) {
    val isDefined: Boolean =
      tournaments.isDefined && tournamentSeries.isDefined && users.isDefined
    def build(): GatheredInformation =
      (for {
        t <- tournaments
        ts <- tournamentSeries
        us <- users
      } yield {
        GatheredInformation(t, ts, us)
      }).getOrElse(throw new IllegalStateException("Information not acquired"))
  }
  object GatheredInformationBuilder {
    def apply(): GatheredInformationBuilder =
      GatheredInformationBuilder(None, None, None)
  }

  def apply(
      gatheredInformationBuilder: GatheredInformationBuilder,
      replyTo: ActorRef[GatheringComplete],
      origin: ActorRef[GatheringSucess]
  ): Behavior[InternalCommand] = {

    def nextState(
        newGathered: GatheredInformationBuilder
    ): Behavior[InternalCommand] =
      if (newGathered.isDefined) {
        replyTo ! GatheringComplete(origin, newGathered.build())
        Behaviors.stopped
      } else {
        WinnersGatheringWorker(newGathered, replyTo, origin)
      }

    Behaviors.receiveMessage {
      case TournamentsGathered(tournaments) =>
        nextState(
          gatheredInformationBuilder.copy(tournaments = Some(tournaments))
        )
      case TournamentSeriesGathered(tournamentSeries) =>
        nextState(
          gatheredInformationBuilder
            .copy(tournamentSeries = Some(tournamentSeries))
        )

      case UsersGathered(users) =>
        nextState(gatheredInformationBuilder.copy(users = Some(users)))

    }
  }
}
