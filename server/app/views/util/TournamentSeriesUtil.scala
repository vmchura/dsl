package views.util

import models.TournamentSeriesFilled

case class TournamentResource(cssClass: String, logo: String)

object TournamentSeriesUtil {

  def getTournamentResource(
      tournamentSeries: TournamentSeriesFilled
  ): TournamentResource = {
    val tr = tournamentSeries.name.toLowerCase() match {
      case name @ _ if name.contains("super") =>
        TournamentResource("DSSL-SERIES", "images/DSSLMedal.png")
      case name @ _
          if name.contains("dsl") &&
            !name.contains("plata") &&
            !name.contains("bronce") &&
            !name.contains("oro") =>
        TournamentResource("DSL-SERIES", "images/DSL.png")
      case name @ _ if name.contains("challenger") =>
        TournamentResource("DCSL-SERIES", "images/DCSL.png")
      case name @ _ if name.contains("oro") =>
        TournamentResource("DSL-ORO-SERIES", "images/OroMedal.png")
      case name @ _ if name.contains("plata") =>
        TournamentResource("DSL-PLATA-SERIES", "images/PlataMedal.png")
      case name @ _ if name.contains("bronce") =>
        TournamentResource("DSL-BRONCE-SERIES", "images/BronceMedal.png")
      case _ => TournamentResource("OTHER-SERIES", "images/silhouette.png")

    }
    tr.copy(cssClass = tr.cssClass + " tournament-series")
  }
}
