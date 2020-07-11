package shared

object UtilParser {

  def safeJson2String(str: String): String = str.replace("\\", "$backslash;")
  def safeString2Json(str: String): String = str.replace("&quot;", "\"").replace("$backslash;", "\\")
}
