package views.util

import models.TournamentSeries

object TournamentSeriesUtil {
  def getCSSClass(tournamentSeries: TournamentSeries): String = {
    tournamentSeries.name.toLowerCase() match {
      case name @ _ if name.contains("super") => "DSSL-SERIES"
      case name @ _
          if name.contains("dsl") &&
            !name.contains("plata") &&
            !name.contains("bronce") &&
            !name.contains("oro") =>
        "DSL-SERIES"
      case name @ _ if name.contains("challenger") => "DCSL-SERIES"
      case name @ _ if name.contains("oro")        => "DSL-ORO-SERIES"
      case name @ _ if name.contains("plata")      => "DSL-PLATA-SERIES"
      case name @ _ if name.contains("bronce")     => "DSL-BRONCE-SERIES"
      case _                                       => "OTHER-SERIES"

    }
  }
}
