import java.util.{Date, UUID}
import play.api.libs.json.{
  JsNumber,
  JsObject,
  JsResult,
  JsString,
  Json,
  OFormat
}
import shared.models.StarCraftModels.{OneVsOne, SCPlayer, SCRace, StringDate}
import shared.models.{
  DiscordID,
  DiscordPlayerLogged,
  ReplayTeamID,
  ReplayTeamRecord
}

import scala.util.Try

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
    implicit val stringDateFormat: OFormat[StringDate] = Json.format[StringDate]

    implicit val replayTeamIDJsonImplicit: OFormat[ReplayTeamID] =
      Json.format[ReplayTeamID]
    implicit val discordIDjsonFormatJsonImplicit: OFormat[DiscordID] =
      Json.format[DiscordID]
    implicit val scRaceFormatJsonImplicit: OFormat[SCRace] =
      OFormat[SCRace](
        jsValue => JsResult.fromTry(Try(SCRace((jsValue \ "race").as[String]))),
        sCRace => JsObject(Map("race" -> JsString(sCRace.str)))
      )
    implicit val scPlayerFormatJsonImplicit: OFormat[SCPlayer] =
      Json.format[SCPlayer]
    implicit val oneVsOnejsonFormatJsonImplicit: OFormat[OneVsOne] =
      Json.format[OneVsOne]
    implicit val replayTeamRecordJsonImplicit: OFormat[ReplayTeamRecord] =
      Json.format[ReplayTeamRecord]
    implicit val discordPlayerLoggedFormatJsonImplicit
        : OFormat[DiscordPlayerLogged] =
      Json.format[DiscordPlayerLogged]

  }
}
