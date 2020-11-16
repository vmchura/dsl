package views.util

import models.TournamentSeriesFilled

case class TournamentResource(cssClass: String, logo: String)

object TournamentSeriesUtil {

  def getTournamentResource(
      tournamentSeries: TournamentSeriesFilled
  ): TournamentResource = {
    tournamentSeries.name.toLowerCase() match {
      case name @ _ if name.contains("super") =>
        TournamentResource("DSSL-SERIES", "images/silhouette.png")
      case name @ _
          if name.contains("dsl") &&
            !name.contains("plata") &&
            !name.contains("bronce") &&
            !name.contains("oro") =>
        TournamentResource("DSL-SERIES", "images/silhouette.png")
      case name @ _ if name.contains("challenger") =>
        TournamentResource("DCSL-SERIES", "images/silhouette.png")
      case name @ _ if name.contains("oro") =>
        TournamentResource("DSL-ORO-SERIES", "images/silhouette.png")
      case name @ _ if name.contains("plata") =>
        TournamentResource("DSL-PLATA-SERIES", "images/silhouette.png")
      case name @ _ if name.contains("bronce") =>
        TournamentResource("DSL-BRONCE-SERIES", "images/silhouette.png")
      case _ => TournamentResource("OTHER-SERIES", "images/silhouette.png")

    }
  }
}
