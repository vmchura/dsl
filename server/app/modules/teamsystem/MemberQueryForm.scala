package modules.teamsystem

import play.api.data.Form
import play.api.data.Forms._

object MemberQueryForm {
  val memberQuery: Form[String] = Form(
    single(
      "query" -> nonEmptyText
    )
  )

}
