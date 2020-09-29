package models

import org.scalatest.FunSuite
import play.api.libs.json.Json

class ValidUserSmurfTest extends FunSuite {

  test("ValidUserSmurf"){
    val smurf = Smurf("VmChQ")
    val id = DiscordID("123123")
    val validSmurf = ValidUserSmurf(id, List(smurf))
    val u = Json.toJson(validSmurf)
    succeed
  }
}
