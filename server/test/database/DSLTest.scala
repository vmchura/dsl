package database

import org.scalatestplus.play.PlaySpec

class DSLTest extends PlaySpec with TemporalDB {

  "Each test" should {
    "load config from config test ad-hoc" in {
      assert(
        app.configuration
          .getOptional[String]("mongodb.uri")
          .exists(_.endsWith("dsl-test"))
      )
    }
  }
}
