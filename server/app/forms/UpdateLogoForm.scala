package forms
import play.api.data.Form
import play.api.data.Forms._

import java.util.UUID
object UpdateLogoForm {

  val form: Form[UpdateLogoData] = Form(
    mapping(
      "teamID" -> uuid,
      "urlImage" -> nonEmptyText
    )(UpdateLogoData.apply)(UpdateLogoData.unapply)
  )

  case class UpdateLogoData(
      teamID: UUID,
      urlImage: String
  )
}
