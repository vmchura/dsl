package forms

import java.util.UUID

import play.api.data.Form
import play.api.data.Forms._

/**
 * The form which handles the sign up process.
 */
object OrderGamesForm {

  /**
   * A play framework form.
   */
  val form: Form[Data] = Form(
    mapping(
          "bof" -> number,
      "replayID" -> list(uuid)
    )(Data.apply)(Data.unapply)
  )

  case class Data(bof: Int,
                  replayID: List[UUID])
}

