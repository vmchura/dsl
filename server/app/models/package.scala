import java.util.UUID
import play.api.libs.json.{Json, OFormat}
import shared.models.{
  DiscordID,
  DiscordPlayerLogged,
  ReplayTeamID,
  ReplayTeamRecord
}

package object models {

  case class GuildID(id: String) extends AnyVal
  object GuildID {
    implicit val jsonFormat: OFormat[GuildID] = Json.format[GuildID]
  }

  case class UserHistoryID(id: UUID) extends AnyVal
  object UserHistoryID {
    implicit val jsonFormat: OFormat[UserHistoryID] = Json.format[UserHistoryID]
  }

  case class Smurf(name: String) extends AnyVal

  object Smurf {
    implicit val jsonFormat: OFormat[Smurf] = Json.format[Smurf]
  }
  object ModelsJsonImplicits {
    import be.venneborg.refined.play.RefinedJsonFormats._
    implicit val replayTeamIDJsonImplicit: OFormat[ReplayTeamID] =
      Json.format[ReplayTeamID]
    implicit val discordIDjsonFormatJsonImplicit: OFormat[DiscordID] =
      Json.format[DiscordID]
    implicit val replayTeamRecordJsonImplicit: OFormat[ReplayTeamRecord] =
      Json.format[ReplayTeamRecord]
    implicit val discordPlayerLoggedFormatJsonImplicit
        : OFormat[DiscordPlayerLogged] =
      Json.format[DiscordPlayerLogged]

  }
}
