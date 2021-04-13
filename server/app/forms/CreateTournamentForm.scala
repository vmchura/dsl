package forms

import play.api.data.Form
import play.api.data.Forms._

/**
  * The form which handles the sign up process.
  */
object CreateTournamentForm {

  /**
    * A play framework form.
    */
  val form: Form[Data] = Form(
    mapping(
      "discordGuildID" -> nonEmptyText,
      "challongeID" -> nonEmptyText,
      "channeldID" -> nonEmptyText,
      "messageID" -> nonEmptyText,
      "discordChannelID" -> text
    )(Data.apply)(Data.unapply)
  )

  case class Data(
      discordGuildID: String,
      challongeID: String,
      channeldID: String,
      messageID: String,
      discordChannelID: String
  )
}
