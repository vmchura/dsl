package modules.teamsystem

import play.api.data.Form
import play.api.data.Forms._

object TeamCreationForm {
  val teamCreation: Form[String] = Form(
    single(
      "teamName" -> nonEmptyText
    )
  )

}
